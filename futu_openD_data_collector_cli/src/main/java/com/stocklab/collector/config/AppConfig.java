package com.stocklab.collector.config;

import com.futu.openapi.pb.TrdCommon;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public class AppConfig {
    private final String opendHost;
    private final int opendPort;
    private final long userId;
    private final long trdAcc;
    private final String unlockTradePwdMd5;
    private final TrdCommon.SecurityFirm securityFirm;
    private final String rsaKeyFile;
    private final MySqlConfig mySqlConfig;

    public AppConfig(String opendHost, int opendPort, long userId, long trdAcc,
                     String unlockTradePwdMd5, TrdCommon.SecurityFirm securityFirm, String rsaKeyFile,
                     MySqlConfig mySqlConfig) {
        this.opendHost = opendHost;
        this.opendPort = opendPort;
        this.userId = userId;
        this.trdAcc = trdAcc;
        this.unlockTradePwdMd5 = unlockTradePwdMd5;
        this.securityFirm = securityFirm;
        this.rsaKeyFile = rsaKeyFile;
        this.mySqlConfig = mySqlConfig;
    }

    public static AppConfig load(Path configPath) throws IOException {
        Properties props = new Properties();
        if (configPath != null && Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                props.load(in);
            }
        } else {
            try (InputStream in = AppConfig.class.getResourceAsStream("/config.properties")) {
                if (in != null) {
                    props.load(in);
                }
            }
        }

        String host = envOrProp("OPEND_HOST", props, "opend.host", "127.0.0.1");
        int port = Integer.parseInt(envOrProp("OPEND_PORT", props, "opend.port", "11111"));
        long userId = Long.parseLong(envOrProp("FUTU_USER_ID", props, "user.id", "0"));
        long trdAcc = Long.parseLong(envOrProp("FUTU_TRD_ACC", props, "trd.acc", "0"));
        String unlockMd5 = envOrProp("FUTU_UNLOCK_MD5", props, "unlock.trade.pwd.md5", "");
        String firmName = envOrProp("FUTU_SECURITY_FIRM", props, "security.firm", "SecurityFirm_FutuSecurities");
        String rsaKey = envOrProp("FUTU_RSA_KEY_FILE", props, "rsa.key.file", "");

        String mysqlHost = envOrProp("MYSQL_HOST", props, "mysql.host", "127.0.0.1");
        int mysqlPort = Integer.parseInt(envOrProp("MYSQL_PORT", props, "mysql.port", "3306"));
        String mysqlDb = envOrProp("MYSQL_DATABASE", props, "mysql.database", "stock_lab");
        String mysqlUser = envOrProp("MYSQL_USER", props, "mysql.user", "root");
        String mysqlPassword = envOrProp("MYSQL_PASSWORD", props, "mysql.password", "");
        MySqlConfig mySqlConfig = new MySqlConfig(mysqlHost, mysqlPort, mysqlDb, mysqlUser, mysqlPassword);

        TrdCommon.SecurityFirm firm;
        try {
            firm = TrdCommon.SecurityFirm.valueOf(firmName);
        } catch (IllegalArgumentException e) {
            firm = TrdCommon.SecurityFirm.SecurityFirm_FutuSecurities;
        }

        return new AppConfig(host, port, userId, trdAcc, unlockMd5, firm, rsaKey, mySqlConfig);
    }

    private static String envOrProp(String envKey, Properties props, String propKey, String defaultValue) {
        String env = System.getenv(envKey);
        if (env != null && !env.isEmpty()) {
            return env;
        }
        return props.getProperty(propKey, defaultValue);
    }

    public static String md5(String value) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(value.getBytes());
            byte[] digest = md5.digest();
            char[] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(hex[(b >>> 4) & 0xF]);
                sb.append(hex[b & 0xF]);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }

    public String getOpendHost() {
        return opendHost;
    }

    public int getOpendPort() {
        return opendPort;
    }

    public long getUserId() {
        return userId;
    }

    public long getTrdAcc() {
        return trdAcc;
    }

    public String getUnlockTradePwdMd5() {
        return unlockTradePwdMd5;
    }

    public TrdCommon.SecurityFirm getSecurityFirm() {
        return securityFirm;
    }

    public String getRsaKeyFile() {
        return rsaKeyFile;
    }

    public MySqlConfig getMySqlConfig() {
        return mySqlConfig;
    }

    public Path resolveConfigPath(String configArg) {
        if (configArg != null && !configArg.isEmpty()) {
            return Paths.get(configArg);
        }
        Path local = Paths.get("config.properties");
        if (Files.exists(local)) {
            return local;
        }
        return null;
    }
}
