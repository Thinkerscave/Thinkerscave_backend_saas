package com.thinkerscave.shared.service;

import com.thinkerscave.shared.dto.AddressDto;
import com.thinkerscave.shared.entity.Address;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Shared address mapping used by modules that store {@link Address} rows.
 * Behaviour mirrors the Student address rules (blank address -> null, default country India).
 */
@Component
public class AddressFactory {

    /** Returns null when no address field carries a value. */
    public Address build(AddressDto dto) {
        if (dto == null) {
            return null;
        }
        if (!hasText(dto.getAddressLine1()) && !hasText(dto.getCity())
                && !hasText(dto.getState()) && !hasText(dto.getPostalCode())) {
            return null;
        }
        Address address = new Address();
        address.setAddressLine1(hasText(dto.getAddressLine1()) ? dto.getAddressLine1().trim() : "-");
        address.setAddressLine2(trimToNull(dto.getAddressLine2()));
        address.setLandmark(trimToNull(dto.getLandmark()));
        address.setCity(trimToNull(dto.getCity()));
        address.setDistrict(trimToNull(dto.getDistrict()));
        address.setState(trimToNull(dto.getState()));
        address.setCountry(hasText(dto.getCountry()) ? dto.getCountry().trim() : "India");
        address.setPostalCode(trimToNull(dto.getPostalCode()));
        address.setActive(true);
        return address;
    }

    /** Creates an independent copy so later edits to one address do not affect the other. */
    public Address copy(Address source) {
        if (source == null) {
            return null;
        }
        Address copy = new Address();
        copy.setAddressLine1(source.getAddressLine1());
        copy.setAddressLine2(source.getAddressLine2());
        copy.setCity(source.getCity());
        copy.setState(source.getState());
        copy.setCountry(source.getCountry());
        copy.setPostalCode(source.getPostalCode());
        copy.setLandmark(source.getLandmark());
        copy.setDistrict(source.getDistrict());
        copy.setActive(true);
        return copy;
    }

    public AddressDto toDto(Address address) {
        if (address == null) {
            return null;
        }
        return AddressDto.builder()
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .landmark(address.getLandmark())
                .city(address.getCity())
                .district(address.getDistrict())
                .state(address.getState())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .build();
    }

    private static boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
