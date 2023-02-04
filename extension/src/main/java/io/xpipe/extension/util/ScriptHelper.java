package io.xpipe.extension.util;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellType;
import io.xpipe.core.process.ShellTypes;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.SecretValue;
import io.xpipe.extension.event.TrackEvent;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Random;

public class ScriptHelper {

    public static int getScriptId() {
        // A deterministic approach can cause permission problems when two different users execute the same command on a
        // system
        // Therefore, use a random approach
        return new Random().nextInt(Integer.MAX_VALUE);
    }

    @SneakyThrows
    public static String createLocalExecScript(String content) {
        try (var l = ShellStore.local().create().start()) {
            return createExecScript(l, content);
        }
    }

    private static final String ZSHI =
            """
          #!/usr/bin/env zsh

          emulate -L zsh -o no_unset

          if (( ARGC == 0 )); then
            print -ru2 -- 'Usage: zshi <init-command> [zsh-flag]...
          The same as plain `zsh [zsh-flag]...` except that an additional
          <init-command> gets executed after all standard Zsh startup files
          have been sourced.'
            return 1
          fi

          () {
            local init=$1
            shift
            local tmp
            {
              tmp=$(mktemp -d ${TMPDIR:-/tmp}/zsh.XXXXXXXXXX) || return
              local rc
              for rc in .zshenv .zprofile .zshrc .zlogin; do
                >$tmp/$rc <<<'{
            if (( ${+_zshi_global_rcs} )); then
              "builtin" "set" "-o" "global_rcs"
              "builtin" "unset" "_zshi_global_rcs"
            fi
            ZDOTDIR="$_zshi_zdotdir"
            # Not .zshenv because /etc/zshenv has already been read
            if [[ -o global_rcs && "'$rc'" != ".zshenv" && -f "/etc/'${rc:1}'" && -r "/etc/'${rc:1}'" ]]; then
              "builtin" "source" "--" "/etc/'${rc:1}'"
            fi
            if [[ -f "$ZDOTDIR/'$rc'" && -r "$ZDOTDIR/'$rc'" ]]; then
              "builtin" "source" "--" "$ZDOTDIR/'$rc'"
            fi
          } always {
            if [[ -o "no_rcs" ||
                  -o "login" && "'$rc'" == ".zlogin" ||
                  -o "no_login" && "'$rc'" == ".zshrc" ||
                  -o "no_login" && -o "no_interactive" && "'$rc'" == ".zshenv" ]]; then
              if (( ${+_zshi_global_rcs} )); then
                set -o global_rcs
              fi
              "builtin" "unset" "_zshi_rcs" "_zshi_zdotdir"
              "builtin" "command" "rm" "-rf" "--" '${(q)tmp}'
              "builtin" "eval" '${(q)init}'
            else
              if [[ -o global_rcs ]]; then
                _zshi_global_rcs=
              fi
              set -o no_global_rcs
              _zshi_zdotdir=${ZDOTDIR:-~}
              ZDOTDIR='${(q)tmp}'
            fi
          }' || return
              done
              _zshi_zdotdir=${ZDOTDIR:-~} ZDOTDIR=$tmp zsh "$@"
            } always {
              [[ -e $tmp ]] && rm -rf -- $tmp
            }
          } "$@"
            """;

    public static String constructOpenWithInitScriptCommand(
            ShellProcessControl processControl, List<String> init, String toExecuteInShell) {
        ShellType t = processControl.getShellType();
        if (init.size() == 0 && toExecuteInShell == null) {
            return t.getNormalOpenCommand();
        }

        String nl = t.getNewLine().getNewLineString();
        var content = String.join(nl, init) + nl;

        if (t.equals(ShellTypes.BASH)) {
            content = "if [ -f ~/.bashrc ]; then . ~/.bashrc; fi\n" + content;
        }

        if (toExecuteInShell != null) {
            // Normalize line endings
            content += String.join(nl, toExecuteInShell.lines().toList()) + nl;
            content += t.getExitCommand() + nl;
        }

        var initFile = createExecScript(processControl, content);

        if (t.equals(ShellTypes.ZSH)) {
            var zshiFile = createExecScript(processControl, ZSHI);
            return t.getNormalOpenCommand() + " \"" + zshiFile + "\" \"" + initFile + "\"";
        }

        return t.getInitFileOpenCommand(initFile);
    }

    @SneakyThrows
    public static String createExecScript(ShellProcessControl processControl, String content) {
        var fileName = "exec-" + getScriptId();
        ShellType type = processControl.getShellType();
        var temp = processControl.getTemporaryDirectory();
        var file = FileNames.join(temp, fileName + "." + type.getScriptFileEnding());
        return createExecScript(processControl, file, content);
    }

    @SneakyThrows
    private static String createExecScript(ShellProcessControl processControl, String file, String content) {
        ShellType type = processControl.getShellType();
        content = type.prepareScriptContent(content);

        TrackEvent.withTrace("proc", "Writing exec script")
                .tag("file", file)
                .tag("content", content)
                .handle();

        // processControl.executeSimpleCommand(type.getFileTouchCommand(file), "Failed to create script " + file);
        processControl.executeSimpleCommand(type.getTextFileWriteCommand(content, file));
        processControl.executeSimpleCommand(
                type.getMakeExecutableCommand(file), "Failed to make script " + file + " executable");
        return file;
    }

    @SneakyThrows
    public static String createAskPassScript(SecretValue pass, ShellProcessControl parent, ShellType type) {
        var content = type.getScriptEchoCommand(pass.getSecretValue());
        var temp = parent.getTemporaryDirectory();
        var file = FileNames.join(temp, "askpass-" + getScriptId() + "." + type.getScriptFileEnding());
        return createExecScript(parent, file, content);
    }
}
