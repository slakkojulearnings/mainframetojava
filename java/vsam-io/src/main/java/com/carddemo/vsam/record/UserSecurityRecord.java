package com.carddemo.vsam.record;

import com.carddemo.codec.TextCodec;

public final class UserSecurityRecord {

    public static final int RECORD_LENGTH = 80;

    private final String userId;
    private final String firstName;
    private final String lastName;
    private final String password;
    private final String userType;
    private final byte[] raw;

    public UserSecurityRecord(String userId, String firstName, String lastName,
                             String password, String userType, byte[] raw) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.userType = userType;
        this.raw = raw;
    }

    public String userId()     { return userId; }
    public String firstName()  { return firstName; }
    public String lastName()   { return lastName; }
    public String password()   { return password; }
    public String userType()   { return userType; }
    public byte[] raw()        { return raw; }

    public byte[] primaryKey() {
        byte[] key = new byte[8];
        System.arraycopy(raw, 0, key, 0, 8);
        return key;
    }

    public byte[] encode() {
        byte[] record = new byte[RECORD_LENGTH];
        byte[] userIdBytes = String.format("%-8s", userId != null ? userId : "").getBytes();
        byte[] firstNameBytes = String.format("%-20s", firstName != null ? firstName : "").getBytes();
        byte[] lastNameBytes = String.format("%-20s", lastName != null ? lastName : "").getBytes();
        byte[] passwordBytes = String.format("%-8s", password != null ? password : "").getBytes();
        byte[] userTypeBytes = (userType != null && !userType.isEmpty()) ? userType.getBytes() : new byte[] { (byte) ' ' };

        System.arraycopy(userIdBytes, 0, record, 0, 8);
        System.arraycopy(firstNameBytes, 0, record, 8, 20);
        System.arraycopy(lastNameBytes, 0, record, 28, 20);
        System.arraycopy(passwordBytes, 0, record, 48, 8);
        System.arraycopy(userTypeBytes, 0, record, 56, 1);

        return record;
    }

    public static UserSecurityRecord decode(byte[] raw) {
        if (raw.length != RECORD_LENGTH) {
            throw new IllegalArgumentException("Expected 80 bytes, got " + raw.length);
        }

        String userId = TextCodec.decode(raw, 0, 8).trim();
        String firstName = TextCodec.decode(raw, 8, 20).trim();
        String lastName = TextCodec.decode(raw, 28, 20).trim();
        String password = TextCodec.decode(raw, 48, 8).trim();
        String userType = TextCodec.decode(raw, 56, 1).trim();

        return new UserSecurityRecord(userId, firstName, lastName, password, userType, raw);
    }

    @Override
    public String toString() {
        return "UserSecurityRecord{" +
               "userId='" + userId + '\'' +
               ", firstName='" + firstName + '\'' +
               ", lastName='" + lastName + '\'' +
               ", userType='" + userType + '\'' +
               '}';
    }
}
