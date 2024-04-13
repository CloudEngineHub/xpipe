## Ba?lama

Sa?lad???n?z ba?lama bilgileri do?rudan `ssh` istemcisine ?u ?ekilde iletilir: `-L [origin_address:]origin_port:remote_address:remote_port`.

Varsay?lan olarak, aksi belirtilmedi?i takdirde kaynak geri d�ng� aray�z�ne ba?lanacakt?r. Ayr?ca, IPv4 �zerinden eri?ilebilen t�m a? aray�zlerine ba?lanmak i�in adresi `0.0.0.0` olarak ayarlamak gibi herhangi bir adres joker karakterinden de yararlanabilirsiniz. Adresi tamamen atlad???n?zda, t�m a? aray�zlerinde ba?lant?lara izin veren `*` joker karakteri kullan?lacakt?r. Baz? a? aray�zleri g�sterimlerinin t�m i?letim sistemlerinde desteklenmeyebilece?ini unutmay?n. �rne?in Windows sunucular? `*` joker karakterini desteklemez.
