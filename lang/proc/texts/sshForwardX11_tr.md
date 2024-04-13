## X11 Y�nlendirme

Bu se�enek etkinle?tirildi?inde, SSH ba?lant?s? X11 y�nlendirme kurulumu ile ba?lat?lacakt?r. Linux'ta bu genellikle kutudan �?kar �?kmaz �al???r ve herhangi bir kurulum gerektirmez. MacOS'ta, yerel makinenizde [XQuartz](https://www.xquartz.org/) gibi bir X11 sunucusunun �al???yor olmas? gerekir.

### Windows �zerinde X11

XPipe, SSH ba?lant?n?z i�in WSL2 X11 yeteneklerini kullanman?za izin verir. Bunun i�in ihtiyac?n?z olan tek ?ey yerel sisteminizde kurulu bir [WSL2](https://learn.microsoft.com/en-us/windows/wsl/install) da??t?m?d?r. XPipe m�mk�nse otomatik olarak uyumlu bir da??t?m se�ecektir, ancak ayarlar men�s�nden ba?ka bir da??t?m da kullanabilirsiniz.

Bu, Windows'a ayr? bir X11 sunucusu kurman?za gerek olmad??? anlam?na gelir. Ancak, yine de bir tane kullan?yorsan?z, XPipe bunu alg?layacak ve o anda �al??an X11 sunucusunu kullanacakt?r.
