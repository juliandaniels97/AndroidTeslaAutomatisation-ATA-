import sys,struct,hashlib
d=open(sys.argv[1],'rb').read()
eocd=d.rfind(b'PK\x05\x06'); cd_off=struct.unpack('<I',d[eocd+16:eocd+20])[0]
assert d[cd_off-16:cd_off]==b'APK Sig Block 42'
size=struct.unpack('<Q',d[cd_off-24:cd_off-16])[0]; start=cd_off-size-8
p=start+8; end=cd_off-24
def lp(b,o):
    n=struct.unpack('<I',b[o:o+4])[0]; return b[o+4:o+4+n],o+4+n
while p<end:
    n,i=struct.unpack('<QI',d[p:p+12]); v=d[p+12:p+8+n]; p+=8+n
    if i in (0x7109871a,0xf05368c0):
        signers,_=lp(v,0); signer,_=lp(signers,0); sd,_=lp(signer,0)
        digests,o=lp(sd,0); certs,o=lp(sd,o); c,_=lp(certs,0)
        print(hex(i),'cert sha256',hashlib.sha256(c).hexdigest())
