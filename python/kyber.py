from sage.all import PolynomialRing, GF, randint, Matrix

# Parameters
msg = "01001000 01100101 01101100 01101100 01101111 00100000 01010111 01101111 01110010 01101100 01100101" # Hello World in binary
bits = [int(b) for b in msg.replace(" ", "")]
q = 3389
n = 256
k = 2

# Ring init
R = PolynomialRing(GF(q), 'X')
X = R.gen()
Rq = R.quotient(X**n + 1, 'x')
x = Rq.gen()

def createRandomNoisePolinomial():
    return Rq([randint(-2, 2) for i in range(n)])

# Can only encrypt at most 256 bits messages
assert len(bits) <= n

# Key Generation
A = Matrix(Rq, k, k, [Rq.random_element() for i in range(k*k)])

s = Matrix(Rq, k, 1, [createRandomNoisePolinomial() for i in range(k)])
e = Matrix(Rq, k, 1, [createRandomNoisePolinomial() for i in range(k)])

t = A * s + e

pKey = (A, t)
sKey = s

# Encryption
msgWiden = Rq([bit * (q // 2) for bit in bits])

r = Matrix(Rq, k, 1, [createRandomNoisePolinomial() for i in range(k)])
e1 = Matrix(Rq, k, 1, [createRandomNoisePolinomial() for i in range(k)])
e2 = createRandomNoisePolinomial()

u = A.T * r + e1
v = (t.T * r)[0,0] + e2 + msgWiden

cipherText = (u, v)

# Decryption
msgDecryptedWiden = v - (s.T * u)[0,0]

coeffs = msgDecryptedWiden.list()

msgDecrypted = []
for i in range(len(bits)):
    c = int(coeffs[i])
    
    # Map coefficient to symmetric range [-q//2, q//2]
    if c > q // 2:
        c -= q
        
    # Distance to 0 is abs(c). Distance to q//2 is abs(abs(c) - q//2)
    if abs(c) > q // 4:
        msgDecrypted.append(1)
    else:
        msgDecrypted.append(0)

print("Decryption successful?", bits == msgDecrypted)
