import numpy as np
from ref import split, pack, luma_i

def fuse_pixels(a,b,frame_index):
    ar,ag,ab=split(a); br,bg,bb=split(b)
    ol=luma_i(ar,ag,ab); nl=luma_i(br,bg,bb)
    ld=np.abs(ol-nl); cd=np.maximum(np.abs(ar-br),np.maximum(np.abs(ag-bg),np.abs(ab-bb)))
    th=np.clip(16+(ol+nl)//28,20,38)
    c1=(ld<=th)&(cd<=th*2); c2=(~c1)&(ld<=th*2)&(cd<=th*3)
    d=frame_index+1
    r1=pack((ar*frame_index+br)//d,(ag*frame_index+bg)//d,(ab*frame_index+bb)//d)
    r2=pack((ar*7+br)//8,(ag*7+bg)//8,(ab*7+bb)//8)
    return np.where(c1,r1,np.where(c2,r2,a))

def shifted(cur,sx,sy):
    H,W=cur.shape
    ys=np.clip(np.arange(H)+sy,0,H-1); xs=np.clip(np.arange(W)+sx,0,W-1)
    return cur[ys][:,xs]

class Bitmap:  # tiny stand-in with strict getPixels/setPixels
    def __init__(s,a): s.a=a.copy()
    def get(s,top,n):
        assert top+n<=s.a.shape[0]; return s.a[top:top+n].copy()
    def set(s,top,rows): s.a[top:top+rows.shape[0]]=rows

def fuse_stripes(base,cur,sx,sy,fi,R=128):
    H,W=base.a.shape; n=(H+R-1)//R; bufs=[None,None]
    def load(k,t):
        top=k*R; rows=min(R,H-top)
        ys=np.clip(np.arange(top,top+rows)+sy,0,H-1)
        bufs[t]=(top,rows,base.get(top,rows),cur[ys].copy())
    load(0,0)
    for k in range(n):
        top,rows,b,c=bufs[k&1]
        res=fuse_pixels(b,c[:,np.clip(np.arange(W)+sx,0,W-1)],fi)   # workers
        if k+1<n: load(k+1,(k+1)&1)                                 # overlapped read
        base.set(top,res)                                           # write after read
    return base.a

rng=np.random.default_rng(3)
H,W=300,120
truth=(rng.integers(40,200,(H,W)))
def frame(): return pack(*(np.clip(truth+rng.normal(0,10,(H,W)),0,255).astype(np.int64) for _ in range(3)))
frames=[frame() for _ in range(8)]
sh=[(0,0),(3,-2),(-5,4),(0,1),(7,7),(-1,0),(2,-6),(0,0)]
# reference: whole-image sequential fusion (original semantics)
ref=frames[0].copy()
for i in range(1,8):
    ref=fuse_pixels(ref,shifted(frames[i],*sh[i]),i)
bm=Bitmap(frames[0])
for i in range(1,8):
    fuse_stripes(bm,frames[i],*sh[i],i,R=128)
print("8-frame fusion, stripes+pipelined == whole-image:",np.array_equal(bm.a,ref))
for R in (1,7,64,1000):
    bm=Bitmap(frames[0])
    for i in range(1,8): fuse_stripes(bm,frames[i],*sh[i],i,R=R)
    print(" stripe rows",R,np.array_equal(bm.a,ref))
