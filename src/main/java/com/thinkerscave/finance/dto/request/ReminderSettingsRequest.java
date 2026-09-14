package com.thinkerscave.finance.dto.request;

import com.thinkerscave.finance.enums.ReminderRuleKey;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReminderSettingsRequest {
    @NotEmpty @Valid
    private List<Rule> rules;

    @Data
    public static class Rule {
        @NotNull
        private ReminderRuleKey ruleKey;
        @NotNull
        private Boolean enabled;
        @NotNull @Min(0)
        private Integer offsetDays;
        private String channelsCsv;
    }
}
