package com.thinkerscave.common.orgm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private String userCode;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private Long mobileNumber;
    private String userName;
    private String password;
    private String address;
    private String city;
    private String state;
    private String parentUserName;
//    private Boolean isBlocked;
//    private Boolean is2faEnabled;
//    private Integer maxDeviceAllow;
//    private Integer attempts;
//    private LocalDateTime lockDateTime;
//    private String secretOperation;
//    private String remarks;

}
