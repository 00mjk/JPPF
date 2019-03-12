/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.example.interceptor;

import java.io.*;
import java.security.KeyStore;

import javax.crypto.*;

import org.jppf.utils.base64.*;
import org.jppf.utils.streams.StreamUtils;

/**
 * This class provides helper methods to create a cipher and its parameters,
 * generate symetric secret keys and create and use a keystore.
 * @author Laurent Cohen
 */
final class CryptoHelper {
  /**
   * The keystore password.
   * This variable will be assigned the password value in clear,
   * after it has been read from a file and decoded from Base64 encoding.
   */
  private static char[] some_chars;
  /**
   * Secret (symmetric) key used for encryption and decryption.
   */
  private static SecretKey secretKey;

  /**
   * Instantiation of this class is not permitted.
   */
  private CryptoHelper() {
  }

  /**
   * Main entry point, creates the keystore.
   * The keystore is then included in the jar file generated by the script.<br/>
   * The keystore password, passed as argument, is encoded in Base64 form, then stored
   * into a file that is also included in the jar file. This ensures that no password
   * in clear is ever deployed.
   * @param args the first argument must be the keystore password in clear.
   */
  public static void main(final String... args) {
    try {
      generateKeyStore(args[0]);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Generate a keystore with a default password.
   * @param pwd default keystore password
   * @throws Exception if any error occurs.
   */
  private static void generateKeyStore(final String pwd) throws Exception {
    final byte[] passwordBytes = pwd.getBytes();
    // encode the password in Base64
    final byte[] encodedBytes = Base64Encoding.encodeBytesToBytes(passwordBytes);
    // store the encoded password to a file
    try (final FileOutputStream fos = new FileOutputStream(getPasswordFilename())) {
      fos.write(encodedBytes);
      fos.flush();
    }
    final char[] password = pwd.toCharArray();
    final KeyStore ks = KeyStore.getInstance(getKeystoreProvider());
    // create an empty keystore
    ks.load(null, password);
    // generate the initial secret key
    final KeyGenerator gen = KeyGenerator.getInstance(getAlgorithm());
    final SecretKey key = gen.generateKey();
    // save the key in the keystore
    final KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(key);
    ks.setEntry(getKeyAlias(), skEntry, new KeyStore.PasswordProtection(password));
    // save the keystore to a file
    try (final FileOutputStream fos = new FileOutputStream(getKeystoreFilename())) {
      ks.store(fos, password);
    }
  }

  /**
   * Get the keystore password.
   * @return the password as a char[].
   */
  static char[] getPassword() {
    if (some_chars == null) {
      try {
        final String path = getKeystoreFolder() + getPasswordFilename();
        final InputStream is = CryptoHelper.class.getClassLoader().getResourceAsStream(path);
        // read the encoded password
        final byte[] encodedBytes = StreamUtils.getInputStreamAsByte(is);
        // decode the password from Base64
        final byte[] passwordBytes = Base64Decoding.decode(encodedBytes);
        some_chars = new String(passwordBytes).toCharArray();
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
    return some_chars;
  }


  /**
   * Get the secret key used for encryption/decryption.
   * In this method, the secret key is read from a location in the classpath.
   * This is definitely unsecure, and for demonstration purposes only.
   * The secret key should be stored in a secure location such as a key store.
   * @return a <code>SecretKey</code> instance.
   */
  private static synchronized SecretKey getSecretKey() {
    if (secretKey == null) {
      try {
        // get the keystore password
        final char[] password = getPassword();
        final ClassLoader cl = CryptoHelper.class.getClassLoader();
        final InputStream is = cl.getResourceAsStream(getKeystoreFolder() + getKeystoreFilename());
        final KeyStore ks = KeyStore.getInstance(getKeystoreProvider());
        // load the keystore
        ks.load(is, password);
        // get the secret key from the keystore
        secretKey = (SecretKey) ks.getKey(getKeyAlias(), password);
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
    return secretKey;
  }

  /**
   * Encrypt the specified string message to the specified output stream.
   * @param message the string to encrypt.
   * @param destination the stream into which the encrypted data is written.
   * @throws Exception if any error occurs while encrypting the data.
   */
  static void encryptAndWrite(final String message, final OutputStream destination) throws Exception {
    // create a cipher instance
    final Cipher cipher = Cipher.getInstance(getTransformation());
    // init the cipher in encryption mode
    cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    // obtain a cipher output stream
    try (final DataOutputStream cos = new DataOutputStream(new CipherOutputStream(baos, cipher))) {
      // finally, encrypt the message
      cos.writeUTF(message);
    }
    final DataOutputStream dos = new DataOutputStream(destination);
    final byte[] encrypted = baos.toByteArray();
    // write the length of the encrypted data
    dos.writeInt(encrypted.length);
    // write the encrypted data
    dos.write(encrypted);
    dos.flush();
  }

  /**
   * Read and decrypt the next string from the specified string.
   * @param source the input stream of data to decrypt.
   * @return the next message in decrypted form as a string.
   * @throws Exception if any error occurs while decrypting the data.
   */
  static String readAndDecrypt(final InputStream source) throws Exception {
    final DataInputStream dis = new DataInputStream(source);
    // read the length of the encrypted data
    final int len = dis.readInt();
    final byte[] encrypted = new byte[len];
    // read the encrypted data
    dis.read(encrypted);
    // decrypt the message using the secret key stored in the keystore
    final Cipher cipher = Cipher.getInstance(getTransformation());
    // init the cipher in decryption mode
    cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
    // obtain a cipher input stream
    try (final DataInputStream cis = new DataInputStream(
      new CipherInputStream(new ByteArrayInputStream(encrypted), cipher))) {
      // finally, decrypt the message
      return cis.readUTF();
    }
  }

  /**
   * Get the password file name.
   * @return the password file name.
   */
  private static String getPasswordFilename() {
    return "password.pwd";
  }

  /**
   * Get the keystore file name.
   * @return the keystore file name.
   */
  private static String getKeystoreFilename() {
    return "keystore.ks";
  }

  /**
   * The folder in which the keystore and password file will be in the jar file.
   * @return the folder name as a string.
   */
  private static String getKeystoreFolder() {
    return "org/jppf/example/interceptor/";
  }

  /**
   * Get the key alias.
   * @return the key alias.
   */
  private static String getKeyAlias() {
    return "secretKeyAlias";
  }

  /**
   * Get the cryptographic provider, or keystore type.
   * @return the provider name.
   */
  public static String getKeystoreProvider() {
    // jceks is the only ootb provider that allows storing a secret key
    return "JCEKS";
  }

  /**
   * Get the name of the cryptographic algorithm used to generate secret keys.
   * @return the algorithm name as a string.
   */
  private static String getAlgorithm() {
    return "DES";
  }

  /**
   * Get the name of the cryptographic transformation used when encrypting or decrypting data.
   * @return the transformation as a string.
   */
  private static String getTransformation() {
    return "DES/ECB/PKCS5Padding";
  }
}
