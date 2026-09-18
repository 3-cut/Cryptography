from random import randint
p = 31
q = 67
n = p*q

def temoin_Miller_Rabin(a,p):
    (k,q)=decompose(p)
    b=pow(a,q,p)
    if b==1 or b==p-1:
        return True
    while k>0:
        b=pow(b,2,p)
        if b==p-1:
            return True
        k=k-1
    return False

def Recupere_premiers(filename):
    f = open(filename,'r')
    premiers = f.readlines()
    f.close()
    return [int(p) for p in premiers]

def Miller_Rabin_40(p):
    l=[2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71,
73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163,
167, 173]
    for a in l:
        if not temoin_Miller_Rabin(a,p):
            return False
    return True

def test_primalite(p, liste_premiers):
    for i in liste_premiers:
        if p==i:
            return True
        elif p%i==0 :
            return False
    return Miller_Rabin_40(p)

def generer_premiers(n,liste_premiers):
    a=randint(0,2**n)
    if a%2==0:
        a+=1
    booleen=True
    while booleen:
        if test_primalite(a,liste_premiers):
            return a
        else:
            a+=2

def Bezout(x,y):
    (u0,u1)=(1,0)
    (v0,v1)=(0,1)
    while y!=0:
        q=x//y
        r=x%y
        (x,y)=(y,r)
        (u0,u1)=(u1,u0-q*u1)
        (v0,v1)=(v1,v0-q*v1)
    if x<0:
        return -x,-u0,-v0
    else:
        return x,u0,v0

def inverse(x,n):
    if pgcd(x,n)!=1:
        print('erreur pas inversible')
        return 0
    else:
        (x,u,v)=Bezout(x,n)
        return u%n
        
def pgcd(x,y):
    x=abs(x)
    y=abs(y)
    while y!=0 :
        r=x%y
        x=y
        y=r
    return x
        
def euler(x,y) :
	return (x-1)*(y-1)

def chiffrement_rsa(m, e, n):
    return m**e % n

def dechiffrement_rsa(c, d, n):
    return c**d % n

if __name__=='__main__' :
	phi_n = euler(p, q)
	while (1):
		e = randint(2, phi_n)
		if ( pgcd(e, phi_n) == 1):
			break
	d = inverse(e, phi_n)
	print("Public Key : (", n, ",", e, ")")
	print("Private Key : (", p, ",", q, ",", d,")")
	
	c = chiffrement_rsa(2, e, n)
	print("Message chiffré :", c)
	print("Message déchiffré :", dechiffrement_rsa(c, d, n))
