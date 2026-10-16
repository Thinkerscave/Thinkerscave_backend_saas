package com.thinkerscave.settings;

import com.thinkerscave.attendance.enums.AttendanceEditingPolicy;
import com.thinkerscave.attendance.enums.AttendanceMode;
import com.thinkerscave.shared.exceptions.BadRequestException;

import java.math.BigDecimal;
import java.time.format.DateTimeParseException;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ModuleSettingCatalog {
    public static final String ATTENDANCE = "ATTENDANCE";
    public static final String ATTENDANCE_MODE = "ATTENDANCE_MODE";
    public static final String LOW_ATTENDANCE_THRESHOLD = "LOW_ATTENDANCE_THRESHOLD";
    public static final String ATTENDANCE_EDITING_POLICY = "ATTENDANCE_EDITING_POLICY";
    public static final String STAFF_LATE_GRACE_MINUTES = "STAFF_LATE_GRACE_MINUTES";
    public static final String MINIMUM_WORKING_HOURS = "MINIMUM_WORKING_HOURS";
    public static final String ALLOW_COPY_PREVIOUS = "ALLOW_COPY_PREVIOUS";
    public static final String FREEZE_AFTER_DAYS = "FREEZE_AFTER_DAYS";
    public static final String ATTENDANCE_WINDOW_START = "ATTENDANCE_WINDOW_START";
    public static final String ATTENDANCE_WINDOW_END = "ATTENDANCE_WINDOW_END";

    private static final List<ModuleSettingDefinition> DEFINITIONS = List.of(
            definition(ATTENDANCE_MODE, ModuleSettingValueType.ENUM, AttendanceMode.DAILY.name(),
                    null, null, List.of(AttendanceMode.DAILY.name()), true),
            definition(LOW_ATTENDANCE_THRESHOLD, ModuleSettingValueType.INTEGER, 75,
                    BigDecimal.ZERO, BigDecimal.valueOf(100), List.of(), true),
            definition(ATTENDANCE_EDITING_POLICY, ModuleSettingValueType.ENUM,
                    AttendanceEditingPolicy.SAME_DAY.name(), null, null,
                    java.util.Arrays.stream(AttendanceEditingPolicy.values()).map(Enum::name).toList(), true),
            definition(STAFF_LATE_GRACE_MINUTES, ModuleSettingValueType.INTEGER, 15,
                    BigDecimal.ZERO, BigDecimal.valueOf(120), List.of(), true),
            definition(MINIMUM_WORKING_HOURS, ModuleSettingValueType.DECIMAL, BigDecimal.valueOf(8),
                    BigDecimal.ONE, BigDecimal.valueOf(24), List.of(), true),
            definition(ALLOW_COPY_PREVIOUS, ModuleSettingValueType.BOOLEAN, true,
                    null, null, List.of(), false),
            definition(FREEZE_AFTER_DAYS, ModuleSettingValueType.INTEGER, 0,
                    BigDecimal.ZERO, BigDecimal.valueOf(Integer.MAX_VALUE), List.of(), false),
                definition(ATTENDANCE_WINDOW_START, ModuleSettingValueType.TIME, LocalTime.of(9, 0),
                    null, null, List.of(), false),
                definition(ATTENDANCE_WINDOW_END, ModuleSettingValueType.TIME, LocalTime.of(17, 0),
                    null, null, List.of(), false)
    );

    private static final Map<String, ModuleSettingDefinition> BY_SCOPE = DEFINITIONS.stream()
            .collect(Collectors.toUnmodifiableMap(
                    definition -> scopeKey(definition.moduleCode(), definition.key()), Function.identity()));

    private ModuleSettingCatalog() {
    }

    public static List<ModuleSettingDefinition> definitionsFor(String moduleCode) {
        String normalizedModule = normalizeModule(moduleCode);
        List<ModuleSettingDefinition> definitions = DEFINITIONS.stream()
                .filter(definition -> definition.moduleCode().equals(normalizedModule))
                .toList();
        if (definitions.isEmpty()) {
            throw new BadRequestException("Unknown settings module: " + normalizedModule);
        }
        return definitions;
    }

    public static List<ModuleSettingDefinition> editableDefinitionsFor(String moduleCode) {
        return definitionsFor(moduleCode).stream().filter(ModuleSettingDefinition::editable).toList();
    }

    public static ModuleSettingDefinition definition(String moduleCode, String key) {
        ModuleSettingDefinition definition = BY_SCOPE.get(scopeKey(moduleCode, key));
        if (definition == null) {
            throw new BadRequestException("Unknown setting key " + key + " for module " + moduleCode);
        }
        return definition;
    }

    public static String normalizeModule(String moduleCode) {
        if (moduleCode == null || moduleCode.isBlank()) {
            throw new BadRequestException("Settings module is required");
        }
        return moduleCode.trim().toUpperCase(Locale.ROOT);
    }

    public static Object validateAndNormalize(ModuleSettingDefinition definition, Object rawValue) {
        if (rawValue == null) {
            throw new BadRequestException("Setting " + definition.key() + " cannot be null");
        }
        try {
            return switch (definition.valueType()) {
                case ENUM -> validateEnum(definition, rawValue);
                case INTEGER -> validateInteger(definition, rawValue);
                case DECIMAL -> validateDecimal(definition, rawValue);
                case BOOLEAN -> validateBoolean(definition, rawValue);
                case TIME -> validateTime(definition, rawValue);
            };
        } catch (NumberFormatException | ClassCastException | DateTimeParseException ex) {
            throw new BadRequestException("Invalid value for setting " + definition.key());
        }
    }

    public static String toStoredValue(ModuleSettingDefinition definition, Object value) {
        Object normalized = validateAndNormalize(definition, value);
        return switch (definition.valueType()) {
            case ENUM -> ((String) normalized).toUpperCase(Locale.ROOT);
            case DECIMAL -> ((BigDecimal) normalized).stripTrailingZeros().toPlainString();
            case TIME -> ((LocalTime) normalized).toString();
            case INTEGER, BOOLEAN -> normalized.toString().toLowerCase(Locale.ROOT);
        };
    }

    public static Object fromStoredValue(ModuleSettingDefinition definition, String value) {
        return validateAndNormalize(definition, switch (definition.valueType()) {
            case ENUM, TIME -> value;
            case INTEGER -> Integer.valueOf(value);
            case DECIMAL -> new BigDecimal(value);
            case BOOLEAN -> switch (value) {
                case "true" -> true;
                case "false" -> false;
                default -> throw new BadRequestException("Invalid stored boolean for setting " + definition.key());
            };
        });
    }

    private static ModuleSettingDefinition definition(String key, ModuleSettingValueType type, Object defaultValue,
            BigDecimal minimum, BigDecimal maximum, List<String> allowedValues, boolean editable) {
        return new ModuleSettingDefinition(ATTENDANCE, key, type, defaultValue,
                minimum, maximum, allowedValues, editable);
    }

    private static String scopeKey(String moduleCode, String key) {
        return normalizeModule(moduleCode) + ":" + (key == null ? "" : key.trim().toUpperCase(Locale.ROOT));
    }

    private static String validateEnum(ModuleSettingDefinition definition, Object rawValue) {
        if (!(rawValue instanceof String value) || !definition.allowedValues().contains(value)) {
            throw new BadRequestException("Unsupported value for setting " + definition.key());
        }
        return value;
    }

    private static Integer validateInteger(ModuleSettingDefinition definition, Object rawValue) {
        BigDecimal value = numericValue(rawValue);
        if (value.stripTrailingZeros().scale() > 0
                || belowMinimum(definition, value) || aboveMaximum(definition, value)) {
            throw new BadRequestException("Value for setting " + definition.key() + " is out of range");
        }
        return value.intValueExact();
    }

    private static BigDecimal validateDecimal(ModuleSettingDefinition definition, Object rawValue) {
        BigDecimal value = numericValue(rawValue).stripTrailingZeros();
        if (belowMinimum(definition, value) || aboveMaximum(definition, value)
                || value.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0) {
            throw new BadRequestException("Value for setting " + definition.key() + " is out of range");
        }
        return value;
    }

    private static Boolean validateBoolean(ModuleSettingDefinition definition, Object rawValue) {
        if (!(rawValue instanceof Boolean value)) {
            throw new BadRequestException("Setting " + definition.key() + " must be true or false");
        }
        return value;
    }

    private static LocalTime validateTime(ModuleSettingDefinition definition, Object rawValue) {
        if (!(rawValue instanceof String value)) {
            throw new BadRequestException("Setting " + definition.key() + " must be a time");
        }
        return LocalTime.parse(value);
    }

    private static BigDecimal numericValue(Object value) {
        if (!(value instanceof Number) && !(value instanceof String)) {
            throw new NumberFormatException();
        }
        return new BigDecimal(value.toString());
    }

    private static boolean belowMinimum(ModuleSettingDefinition definition, BigDecimal value) {
        return definition.minimum() != null && value.compareTo(definition.minimum()) < 0;
    }

    private static boolean aboveMaximum(ModuleSettingDefinition definition, BigDecimal value) {
        return definition.maximum() != null && value.compareTo(definition.maximum()) > 0;
    }
}