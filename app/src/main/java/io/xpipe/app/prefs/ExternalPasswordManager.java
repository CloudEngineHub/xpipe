package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.OsType;

import java.util.List;
import java.util.stream.Stream;

public interface ExternalPasswordManager extends PrefsChoiceValue {

    String retrievePassword(String key);

    ExternalPasswordManager COMMAND = new ExternalPasswordManager() {

        @Override
        public String retrievePassword(String key) {
            var cmd = AppPrefs.get().passwordManagerString(key);
            if (cmd == null) {
                return null;
            }

            try (var cc = ProcessControlProvider.get().createLocalProcessControl(true).command(cmd).start()) {
                var out = cc.readStdoutOrThrow();

                // Dashlane fixes
                var rawCmd = AppPrefs.get().passwordManagerCommand.get();
                if (rawCmd.contains("dcli")) {
                    out = out.lines().findFirst().map(s -> s.trim().replaceAll("\\s+$", "")).orElse(null);
                }

                return out;
            } catch (Exception ex) {
                ErrorEvent.fromThrowable("Unable to retrieve password with command " + cmd, ex)
                        .expected()
                        .handle();
                return null;
            }
        }

        @Override
        public String getId() {
            return "command";
        }
    };

    ExternalPasswordManager WINDOWS_CREDENTIAL_MANAGER = new ExternalPasswordManager() {

        private boolean loaded = false;

        @Override
        public synchronized String retrievePassword(String key) {
            try {
                if (!loaded) {
                    loaded = true;
                    var cmd =
                            """
                   $code = @"
                   using System.Text;
                   using System;
                   using System.Runtime.InteropServices;

                   namespace CredManager {
                     [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
                     public struct CredentialMem
                     {
                       public int flags;
                       public int type;
                       public string targetName;
                       public string comment;
                       public System.Runtime.InteropServices.ComTypes.FILETIME lastWritten;
                       public int credentialBlobSize;
                       public IntPtr credentialBlob;
                       public int persist;
                       public int attributeCount;
                       public IntPtr credAttribute;
                       public string targetAlias;
                       public string userName;
                     }

                     public class Credential {
                       [DllImport("advapi32.dll", EntryPoint = "CredReadW", CharSet = CharSet.Unicode, SetLastError = true)]
                       private static extern bool CredRead(string target, int type, int reservedFlag, out IntPtr credentialPtr);

                       public static string GetUserPassword(string target)
                       {
                         CredentialMem credMem;
                         IntPtr credPtr;

                         if (CredRead(target, 1, 0, out credPtr))
                         {
                           credMem = Marshal.PtrToStructure<CredentialMem>(credPtr);
                           byte[] passwordBytes = new byte[credMem.credentialBlobSize];
                           Marshal.Copy(credMem.credentialBlob, passwordBytes, 0, credMem.credentialBlobSize);
                           return Encoding.Unicode.GetString(passwordBytes);
                         } else {
                           throw new Exception("No credentials found for target: " + target);
                         }
                       }
                     }
                   }
                   "@
                   Add-Type -TypeDefinition $code -Language CSharp
                   """;
                    LocalShell.getLocalPowershell().command(cmd).execute();
                }

                return LocalShell.getLocalPowershell()
                        .command("[CredManager.Credential]::GetUserPassword(\"" + key.replaceAll("\"", "`\"") + "\")")
                        .readStdoutOrThrow();
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).expected().handle();
                return null;
            }
        }

        @Override
        public String getId() {
            return "windowsCredentialManager";
        }

        @Override
        public boolean isSelectable() {
            return OsType.getLocal() == OsType.WINDOWS;
        }
    };

    List<ExternalPasswordManager> ALL = Stream.of(COMMAND, WINDOWS_CREDENTIAL_MANAGER)
            .filter(externalPasswordManager -> externalPasswordManager.isSelectable())
            .toList();
}
