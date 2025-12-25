package com.thinkerscave.common.usrm.mapper;

import com.thinkerscave.common.orgm.dto.OrgRequestDTO;
import com.thinkerscave.common.orgm.service.serviceImp.OrganizationServiceImpl;
import com.thinkerscave.common.usrm.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class UserMapper {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationServiceImpl.class);

    /**
     * Builds a User entity with common attributes.
     */
    public static User buildUser(
            OrgRequestDTO request,
            String username,
            String encodedPassword,
            String schema) {

        logger.debug("🛠️ [User-Builder] Building user object | email={}",
                request.getOwnerEmail());

        User user = new User();
        user.setFirstName(request.getOwnerName());
        user.setLastName(""); // Can be enhanced later
        user.setEmail(request.getOwnerEmail());
        user.setMobileNumber(Long.parseLong(request.getOwnerMobile()));
        user.setUserName(username);
        user.setPassword(encodedPassword);
        user.setSchemaName(schema);
        user.setUserCode(generateUserCode());

        return user;
    }

    /**
     * Generates unique user code.
     */
    private static String generateUserCode() {
        return "USER-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }

}
