package net.javacoding.jspider.core.util;


/**
 * This class is derived from the one found in the JavaWorld.com article
 * http://www.javaworld.com/javaworld/javatips/jw-javatip36-p2.html
 *
 * $Id: Base64Encoder.java,v 1.2 2003/02/11 17:27:04 vanrogu Exp $
 *
 */
public abstract class Base64Encoder {

    public final static String base64Encode(String strInput) {
        if (strInput == null) return null;

        byte byteData[] = new byte[strInput.length()];
        byteData = strInput.getBytes();
        return new String(base64Encode(byteData));
    }

    public final static byte[] base64Encode(byte[] byteData) {
        if (byteData == null) return null;
        int iSrcIdx;      // index into source (byteData)
        int iDestIdx;     // index into destination (byteDest)
        byte byteDest[] = new byte[((byteData.length + 2) / 3) * 4];
        for (iSrcIdx = 0, iDestIdx = 0; iSrcIdx < byteData.length - 2; iSrcIdx += 3) {
            byteDest[iDestIdx++] = (byte) ((byteData[iSrcIdx] >>> 2) & 077);
            byteDest[iDestIdx++] = (byte) ((byteData[iSrcIdx + 1] >>> 4) & 017 |
                    (byteData[iSrcIdx] << 4) & 077);
            byteDest[iDestIdx++] = (byte) ((byteData[iSrcIdx + 2] >>> 6) & 003 |
                    (byteData[iSrcIdx + 1] << 2) & 077);
            byteDest[iDestIdx++] = (byte) (byteData[iSrcIdx + 2] & 077);
        }

        if (iSrcIdx < byteData.length) {
            byteDest[iDestIdx++] = (byte) ((byteData[iSrcIdx] >>> 2) & 077);
            if (iSrcIdx < byteData.length - 1) {
                byteDest[iDestIdx++] = (byte) ((byteData[iSrcIdx + 1] >>> 4) & 017 |
                        (byteData[iSrcIdx] << 4) & 077);
                byteDest[iDestIdx++] = (byte) ((byteData[iSrcIdx + 1] << 2) & 077);
            } else
                byteDest[iDestIdx++] = (byte) ((byteData[iSrcIdx] << 4) & 077);
        }

        for (iSrcIdx = 0; iSrcIdx < iDestIdx; iSrcIdx++) {
            if (byteDest[iSrcIdx] < 26)
                byteDest[iSrcIdx] = (byte) (byteDest[iSrcIdx] + 'A');
            else if (byteDest[iSrcIdx] < 52)
                byteDest[iSrcIdx] = (byte) (byteDest[iSrcIdx] + 'a' - 26);
            else if (byteDest[iSrcIdx] < 62)
                byteDest[iSrcIdx] = (byte) (byteDest[iSrcIdx] + '0' - 52);
            else if (byteDest[iSrcIdx] < 63)
                byteDest[iSrcIdx] = '+';
            else
                byteDest[iSrcIdx] = '/';
        }

        for (; iSrcIdx < byteDest.length; iSrcIdx++)
            byteDest[iSrcIdx] = '=';
        return byteDest;
    }
}