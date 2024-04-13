### SSH yap?land?rmalar?

XPipe t�m ana bilgisayarlar? y�kler ve se�ilen dosyada yap?land?rd???n?z t�m ayarlar? uygular. Dolay?s?yla, bir yap?land?rma se�ene?ini genel veya ana bilgisayara �zel olarak belirtti?inizde, XPipe taraf?ndan kurulan ba?lant?ya otomatik olarak uygulanacakt?r.

SSH yap?land?rmalar?n?n nas?l kullan?laca?? hakk?nda daha fazla bilgi edinmek istiyorsan?z, `man ssh_config` kullanabilir veya bu [k?lavuzu] (https://www.ssh.com/academy/ssh/config) okuyabilirsiniz.

### Kimlikler

Burada bir `IdentityFile` se�ene?i de belirtebilece?inizi unutmay?n. Burada herhangi bir kimlik belirtilirse, daha sonra a?a??da belirtilen herhangi bir kimlik g�z ard? edilecektir.

### X11 y�nlendirme

Burada X11 iletimi i�in herhangi bir se�enek belirtilirse, XPipe otomatik olarak WSL arac?l???yla Windows �zerinde X11 iletimi kurmaya �al??acakt?r.