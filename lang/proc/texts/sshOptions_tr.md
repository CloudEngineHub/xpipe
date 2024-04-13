## SSH yap?land?rmalar?

Burada ba?lant?ya aktar?lmas? gereken SSH se�eneklerini belirtebilirsiniz.
`HostName` gibi baz? se�enekler esasen ba?ar?l? bir ba?lant? kurmak i�in gereklidir,
di?er bir�ok se�enek tamamen iste?e ba?l?d?r.

T�m olas? se�eneklere genel bir bak?? elde etmek i�in [`man ssh_config`](https://linux.die.net/man/5/ssh_config) adresini kullanabilir veya bu [k?lavuz](https://www.ssh.com/academy/ssh/config) adresini okuyabilirsiniz.
Desteklenen se�eneklerin tam miktar? tamamen kurulu SSH istemcinize ba?l?d?r.

### Bi�imlendirme

Buradaki i�erik, SSH yap?land?rma dosyas?ndaki bir ana bilgisayar b�l�m�ne e?de?erdir.
`Host` anahtar?n? a�?k�a tan?mlamak zorunda olmad???n?z? unutmay?n, ��nk� bu otomatik olarak yap?lacakt?r.

Birden fazla ana bilgisayar b�l�m� tan?mlamak istiyorsan?z, �rne?in ba?ka bir yap?land?rma ana bilgisayar?na ba?l? bir proxy atlama ana bilgisayar? gibi ba??ml? ba?lant?lar varsa, burada da birden fazla ana bilgisayar giri?i tan?mlayabilirsiniz. XPipe daha sonra ilk ana bilgisayar giri?ini ba?latacakt?r.

Bo?luk veya girinti ile herhangi bir bi�imlendirme yapman?z gerekmez, �al??mas? i�in buna gerek yoktur.

Bo?luk i�eriyorsa herhangi bir de?eri al?nt?lamaya dikkat etmeniz gerekti?ini unutmay?n, aksi takdirde yanl?? aktar?l?rlar.

### Kimlik dosyalar?

Burada bir `IdentityFile` se�ene?i de belirtebilece?inizi unutmay?n.
Bu se�enek burada belirtilirse, daha sonra a?a??da belirtilen herhangi bir anahtar tabanl? kimlik do?rulama se�ene?i g�z ard? edilecektir.

XPipe git kasas?nda y�netilen bir kimlik dosyas?na ba?vurmay? tercih ederseniz, bunu da yapabilirsiniz.
XPipe payla??lan kimlik dosyalar?n? tespit edecek ve git kasas?n? klonlad???n?z her sistemde dosya yolunu otomatik olarak uyarlayacakt?r.
