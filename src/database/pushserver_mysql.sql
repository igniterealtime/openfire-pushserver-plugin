CREATE TABLE ofPushServer (
    domain      VARCHAR(64)             NOT NULL,
    deviceId    VARCHAR(64)             NOT NULL,
    token       VARCHAR(1024)           DEFAULT NULL,
    node        VARCHAR(1024)           DEFAULT NULL,
    secret      VARCHAR(1024)           DEFAULT NULL,
    type        ENUM('ios', 'android')  NOT NULL,
    PRIMARY KEY (domain, deviceId)
);

INSERT INTO ofVersion (name, version) VALUES ('pushserver', 1);