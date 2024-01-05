package de.martinreinhardt.cordova.plugins;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.cordova.engine.SystemWebViewEngine;
import org.apache.cordova.engine.SystemWebViewClient;

import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertificatesCordovaWebViewClient extends SystemWebViewClient {

    /**
     * Logging Tag
     */
    public static final String TAG = "CertificatesCordovaWebViewClient";

    private boolean allowUntrusted = false;

    private String trustedThumbprint = null;

    private String latestRequestThumbprint = null;

    public CertificatesCordovaWebViewClient(SystemWebViewEngine parentEngine) {
        super(parentEngine);
    }

    /**
     * @return true of usage of untrusted (self-signed) certificates is allowed,
     *         otherwise false
     */
    public boolean isAllowUntrusted() {
        return allowUntrusted;
    }

    /**
     *
     *
     * @param pAllowUntrusted
     *            the allowUntrusted to set
     */
    public void setAllowUntrusted(final boolean pAllowUntrusted) {
        this.allowUntrusted = pAllowUntrusted;
    }

    public String getTrustedThumbprint() {
        return trustedThumbprint;
    }
    public void setTrustedThumbprint(final String thumbprint) {
        this.trustedThumbprint = thumbprint;
    }

    public String getLatestRequestThumbprint() {
        return this.latestRequestThumbprint;
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler,
                                   SslError error) {
        Log.d(TAG, "onReceivedSslError. Proceed? " + isAllowUntrusted());
        if (isAllowUntrusted()) {
            handler.proceed();
        } else {
            SslCertificate sslCert = error.getCertificate();
            X509Certificate xcert = null;
            if (sslCert != null) {
                Bundle bundle = SslCertificate.saveState(sslCert);
                byte[] bytes = bundle.getByteArray("x509-certificate");
                if (bytes != null) {
                    try {
                        CertificateFactory certFact = CertificateFactory.getInstance("X.509");
                        Certificate cert = certFact.generateCertificate(new ByteArrayInputStream(bytes));
                        xcert = (X509Certificate) cert;
                    } catch (CertificateException e) {
                        // do nothing, xcert is null
                    }
                }
            }

            if (xcert != null) {
                try {
                    String thumbprint = new String(Hex.encodeHex(DigestUtils.sha256(xcert.getEncoded())));
                    this.latestRequestThumbprint = thumbprint;
                    if (thumbprint.equals(this.trustedThumbprint)) {
                        Log.i(TAG, "Thumbprint matched.");
                        handler.proceed();
                    } else {
                        Log.e(TAG, "Thumbprint not matching. Trusted is " + this.trustedThumbprint + ", received is " + thumbprint);
                    }
                } catch (CertificateEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
            super.onReceivedSslError(view, handler, error);
        }
    }
}
