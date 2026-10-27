/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.mail;

import org.apache.commons.mail.EmailConstants;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import xyz.kyngs.librelogin.api.mail.EmailHandler;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;
import xyz.kyngs.librelogin.common.config.ConfigurationKeys;
import xyz.kyngs.librelogin.common.util.GeneralUtil;

public class AuthenticEMailHandler implements EmailHandler {

    private final AuthenticLibreLogin<?, ?> plugin;

    public AuthenticEMailHandler(AuthenticLibreLogin<?, ?> plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendEmail(String email, String subject, String content) {
        try {
            var config = plugin.getConfiguration();
            var port = config.get(ConfigurationKeys.MAIL_PORT);

            var mail = new HtmlEmail();

            mail.setCharset(EmailConstants.UTF_8);
            mail.setHostName(config.get(ConfigurationKeys.MAIL_HOST));
            mail.setSmtpPort(port);
            mail.setSubject(subject);
            mail.setAuthentication(config.get(ConfigurationKeys.MAIL_USERNAME), config.get(ConfigurationKeys.MAIL_PASSWORD));
            mail.addTo(email);
            mail.setFrom(config.get(ConfigurationKeys.MAIL_EMAIL), config.get(ConfigurationKeys.MAIL_SENDER));

            switch (port) {
                case 465 -> {
                    // Port 465 is implicit TLS. Disabling SSL here (as older versions did) sent the SMTP
                    // credentials and password reset tokens over the network in plaintext.
                    mail.setSSLOnConnect(true);
                    mail.setSSLCheckServerIdentity(true);
                }
                default -> {
                    // Everything else is expected to upgrade with STARTTLS. Requiring it prevents a
                    // downgrade to plaintext if the server does not offer TLS.
                    mail.setSSLOnConnect(false);
                    mail.setStartTLSEnabled(true);
                    mail.setStartTLSRequired(true);
                    mail.setSSLCheckServerIdentity(true);
                }
            }

            mail.setHtmlMsg(content);
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            mail.send();
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void sendTestMail(String email) {
        sendEmail(email, "TemsLogin test mail", """
                Congratulations! You have successfully configured email sending in TemsLogin!<br>
                Now, your users can reset their passwords.<br>
                <i>If you have no idea what this means, block the sender.</i>
                """);
    }

    @Override
    public void sendPasswordResetMail(String email, String token, String username, String ip) {
        var server = GeneralUtil.escapeHtml(plugin.getConfiguration().get(ConfigurationKeys.MAIL_SENDER));

        sendEmail(email,
                plugin.getMessages().getRawMessage("email-password-reset-subject")
                        .replace("%server%", server),
                plugin.getMessages().getRawMessage("email-password-reset-content")
                        .replace("%server%", server)
                        .replace("%code%", GeneralUtil.escapeHtml(token))
                        .replace("%ip%", GeneralUtil.escapeHtml(ip))
                        .replace("%name%", GeneralUtil.escapeHtml(username))
        );
    }

    @Override
    public void sendVerificationMail(String email, String token, String username) {
        var server = GeneralUtil.escapeHtml(plugin.getConfiguration().get(ConfigurationKeys.MAIL_SENDER));

        sendEmail(email,
                plugin.getMessages().getRawMessage("email-verification-subject")
                        .replace("%server%", server),
                plugin.getMessages().getRawMessage("email-verification-content")
                        .replace("%name%", GeneralUtil.escapeHtml(username))
                        .replace("%server%", server)
                        .replace("%code%", GeneralUtil.escapeHtml(token))
        );
    }
}
