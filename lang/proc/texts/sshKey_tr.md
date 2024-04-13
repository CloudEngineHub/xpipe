### Yok

`publickey` kimlik do?rulamas?n? devre d??? b?rak?r.

### SSH-Agent

Kimliklerinizin SSH-Agent'ta depolanmas? durumunda, ssh y�r�t�lebilir dosyas?, agent ba?lat?ld???nda bunlar? kullanabilir.
XPipe, hen�z �al??m?yorsa arac? s�recini otomatik olarak ba?latacakt?r.

### Pageant (Windows)

Windows �zerinde pageant kullan?yorsan?z, XPipe �nce pageant'?n �al???p �al??mad???n? kontrol edecektir.
Pageant'?n do?as? gere?i, pageant'a sahip olmak sizin sorumlulu?unuzdad?r
her seferinde eklemek istedi?iniz t�m anahtarlar? manuel olarak belirtmeniz gerekti?inden �al???yor.
E?er �al???yorsa, XPipe uygun adland?r?lm?? boruyu
`-oIdentityAgent=...` ssh i�in, herhangi bir �zel yap?land?rma dosyas? eklemeniz gerekmez.

OpenSSH istemcisinde sorunlara neden olabilecek baz? uygulama hatalar? oldu?unu unutmay?n
kullan?c? ad?n?z bo?luk i�eriyorsa veya �ok uzunsa, en son s�r�m� kullanmaya �al???n.

### Pageant (Linux ve macOS)

Kimliklerinizin pageant arac?s?nda saklanmas? durumunda, arac? ba?lat?l?rsa ssh y�r�t�lebilir dosyas? bunlar? kullanabilir.
XPipe, hen�z �al??m?yorsa arac? s�recini otomatik olarak ba?latacakt?r.

### Kimlik dosyas?

?ste?e ba?l? bir parola ile bir kimlik dosyas? da belirtebilirsiniz.
Bu se�enek `ssh -i <dosya>` se�ene?ine e?de?erdir.

Bunun genel de?il *�zel* anahtar olmas? gerekti?ini unutmay?n.
E?er bunu kar??t?r?rsan?z, ssh size sadece ?ifreli hata mesajlar? verecektir.

### GPG Agent

Kimlikleriniz �rne?in bir ak?ll? kartta saklan?yorsa, bunlar? SSH istemcisine `gpg-agent` arac?l???yla sa?lamay? se�ebilirsiniz.
Bu se�enek, hen�z etkinle?tirilmemi?se arac?n?n SSH deste?ini otomatik olarak etkinle?tirecek ve GPG arac? arka plan program?n? do?ru ayarlarla yeniden ba?latacakt?r.

### Yubikey PIV

Kimlikleriniz Yubikey'in PIV ak?ll? kart i?levi ile saklan?yorsa, ?unlar? geri alabilirsiniz
yubico PIV Arac? ile birlikte gelen Yubico'nun YKCS11 k�t�phanesi ile.

Bu �zelli?i kullanabilmek i�in g�ncel bir OpenSSH yap?s?na ihtiyac?n?z oldu?unu unutmay?n.

### �zel ajan

Burada soket konumunu veya adland?r?lm?? boru konumunu sa?layarak �zel bir arac? da kullanabilirsiniz.
Bu, `IdentityAgent` se�ene?i arac?l???yla aktar?lacakt?r.

### �zel PKCS#11 k�t�phanesi

Bu, OpenSSH istemcisine kimlik do?rulamas?n? ger�ekle?tirecek olan belirtilen payla??lan k�t�phane dosyas?n? y�klemesi talimat?n? verecektir.

Bu �zelli?i kullanabilmek i�in g�ncel bir OpenSSH yap?s?na ihtiyac?n?z oldu?unu unutmay?n.
