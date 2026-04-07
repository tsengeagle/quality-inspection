package com.tzuchi.his.quality.inspection.infrastructure.security;

import java.util.List;

/**
 * Deserialized representation of the tz-user claim from the JWT.
 * Structure: {"user":{"user_id":"...","user_name":"...","user_role":[{"role_name":"hospital_manager","enabled":true}]}}
 */
public class ParsedJwtUser {

    private User user;

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public static class User {
        private String user_id;
        private String user_name;
        private List<UserRole> user_role;
        private String hospital_id;

        public String getUser_id() { return user_id; }
        public void setUser_id(String user_id) { this.user_id = user_id; }
        public String getUser_name() { return user_name; }
        public void setUser_name(String user_name) { this.user_name = user_name; }
        public List<UserRole> getUser_role() { return user_role; }
        public void setUser_role(List<UserRole> user_role) { this.user_role = user_role; }
        public String getHospital_id() { return hospital_id; }
        public void setHospital_id(String hospital_id) { this.hospital_id = hospital_id; }
    }

    public static class UserRole {
        private String role_name;
        private boolean enabled;

        public String getRole_name() { return role_name; }
        public void setRole_name(String role_name) { this.role_name = role_name; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
