## Ba?lama

Sa?lad???n?z ba?lama bilgileri do?rudan `ssh` istemcisine ?u ?ekilde aktar?l?r: `-R [remote_source_address:]remote_source_port:origin_destination_address:origin_destination_port`.

Varsay?lan olarak, uzak kaynak adresi geri d�ng� aray�z�ne ba?lanacakt?r. Ayr?ca herhangi bir adres joker karakterini de kullanabilirsiniz, �rne?in IPv4 �zerinden eri?ilebilen t�m a? aray�zlerine ba?lanmak i�in adresi `0.0.0.0` olarak ayarlayabilirsiniz. Adresi tamamen atlad???n?zda, t�m a? aray�zlerinde ba?lant?lara izin veren `*` joker karakteri kullan?lacakt?r. Baz? a? aray�zleri g�sterimlerinin t�m i?letim sistemlerinde desteklenmeyebilece?ini unutmay?n. �rne?in Windows sunucular? `*` joker karakterini desteklemez.
