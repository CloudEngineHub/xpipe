## �zel kabuk ba?lant?lar?

Se�ilen ana bilgisayar sisteminde verilen komutu �al??t?rarak �zel komutu kullanarak bir kabuk a�ar. Bu kabuk yerel ya da uzak olabilir.

Bu i?levin kabu?un `cmd`, `bash`, vb. gibi standart bir t�rde olmas?n? bekledi?ini unutmay?n. Bir terminalde ba?ka t�rde kabuklar ve komutlar a�mak istiyorsan?z, bunun yerine �zel terminal komut t�r�n� kullanabilirsiniz. Standart kabuklar? kullanmak, bu ba?lant?y? dosya taray?c?s?nda da a�man?za olanak tan?r.

### ?nteraktif istemler

Beklenmedik bir gereklilik olmas? durumunda kabuk s�reci zaman a??m?na u?rayabilir veya ask?da kalabilir
giri? istemi, parola istemi gibi. Bu nedenle, her zaman etkile?imli giri? istemleri olmad???ndan emin olmal?s?n?z.

�rne?in, `ssh user@host` gibi bir komut, parola gerekmedi?i s�rece burada iyi �al??acakt?r.

### �zel yerel kabuklar

Bir�ok durumda, baz? komut dosyalar?n?n ve komutlar?n d�zg�n �al??mas?n? sa?lamak i�in genellikle varsay?lan olarak devre d??? b?rak?lan belirli se�eneklerle bir kabuk ba?latmak yararl?d?r. �rne?in:

-   [Gecikmeli Geni?leme
    cmd](https://ss64.com/nt/delayedexpansion.html)
-   [Powershell y�r�tme
    policies](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-7.3)
-   [Bash POSIX
    Mod](https://www.gnu.org/software/bash/manual/html_node/Bash-POSIX-Mode.html)
- Ve se�ti?iniz bir kabuk i�in di?er olas? f?rlatma se�enekleri

Bu, �rne?in a?a??daki komutlarla �zel kabuk komutlar? olu?turularak ger�ekle?tirilebilir:

-   `cmd /v`
-   `powershell -ExecutionMode Bypass`
-   `bash --posix`