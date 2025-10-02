package no.imr.tools.parameter;

import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import org.dom4j.Element;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.logging.Level;

/**
 * For entering a password.
 */
public class PasswordParameter extends ValueParameter<String> {
   public PasswordParameter(Name name) {
      this(name, "", "");
   }

   public PasswordParameter(Name name, String initialValue) {
      this(name, initialValue, "");
   }

   public PasswordParameter(Name name, String initialValue, String description) {
      super(name, initialValue, Unit.NONE, ValueConverters.STRING, description);

      setPersistable(false); // Not persistable by default.
   }

   private static byte[] crypt(int mode, byte[] bytes) throws GeneralSecurityException {
      byte[] desKeyData = {(byte) 0x68, (byte) 0x14, (byte) 0x06, (byte) 0xe6, (byte) 0x90, (byte) 0xcc, (byte) 0x47, (byte) 0x4e};
      Cipher cipher = Cipher.getInstance("DES");
      cipher.init(mode, new SecretKeySpec(desKeyData, "DES"));
      return cipher.doFinal(bytes);
   }

   @Override
   public Element toXml() {
      Element element = createElement();
      String password = getValue();
      if (!password.isEmpty()) {
         try {
            byte[] plaintext = password.getBytes(Utils.UTF_8);
            byte[] encrypted = crypt(Cipher.ENCRYPT_MODE, plaintext);
            String text = HexFormat.of().formatHex(encrypted);
            element.addAttribute("encrypted", "true")
                  .addText(text);
         } catch (GeneralSecurityException e) {
            Log.global.log(Level.WARNING, "Error encrypting password, not stored", e);
         }
      }
      return element;
   }

   @Override
   public String xmlToValue(Element element) {
      try {
         String text = element.getText();
         if (text.isEmpty()) {
            return text;
         }
         byte[] encrypted = HexFormat.of().parseHex(text);
         byte[] plaintext = crypt(Cipher.DECRYPT_MODE, encrypted);
         return new String(plaintext, Utils.UTF_8);
      } catch (GeneralSecurityException e) {
         throw new ParameterException(this, "Error decrypting password", e);
      }
   }
}
