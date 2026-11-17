import numpy as np

def luma_i(r,g,b): return (r*3+g*6+b)//10

def split(img):
    return (img>>16)&255, (img>>8)&255, img&255
def pack(r,g,b):
    return (0xFF<<24)|((r&255)<<16)|((g&255)<<8)|(b&255)

def rint(x):  # Kotlin Float.roundToInt (round half up)
    return np.floor(x.astype(np.float32)+np.float32(0.5)).astype(np.int64)

def detail_core(L, C, pad):
    """L: luma rows incl. halo (H+2,W) int; C: colour rows (H+2,W). returns packed (H,W)"""
    f=np.float32
    Lf=L.astype(f)
    H=L.shape[0]-2; W=L.shape[1]
    ctr=Lf[1:H+1]
    xl=np.concatenate([np.arange(1),np.arange(W-1)]); xr=np.concatenate([np.arange(1,W),[W-1]])
    left=Lf[1:H+1][:,xl]; right=Lf[1:H+1][:,xr]
    up=Lf[0:H]; down=Lf[2:H+2]
    cross=(left+right+up+down)*f(0.25)
    ul=Lf[0:H][:,xl]; ur=Lf[0:H][:,xr]; dl=Lf[2:H+2][:,xl]; dr=Lf[2:H+2][:,xr]
    diag=(ul+ur+dl+dr)*f(0.25)
    ed=ctr-cross; fd=ctr-diag
    em=np.abs(ed)
    cond=(ctr>=f(18))&(ctr<=f(238))&(em>=f(2.5))&(em<=f(58))
    corr=np.where(cond, ed*f(0.24)+np.where(np.abs(fd)>=f(3.5), fd*f(0.10), f(0)), f(0))
    stack=np.stack([ctr,left,right,up,down,ul,ur,dl,dr])
    lmin=stack.min(0); lmax=stack.max(0)
    lim=np.minimum(f(7), np.maximum(em*f(0.48), f(2)))
    cc=np.clip(corr,-lim,lim)
    lo=np.maximum(lmin-f(3),f(0)); hi=np.minimum(lmax+f(3),f(255))
    tgt=np.minimum(np.maximum(ctr+cc,lo),hi)
    gain=np.where(ctr>f(1), tgt/np.where(ctr>f(1),ctr,f(1)), f(1))
    r,g,b=split(C[1:H+1])
    R=np.clip(rint(r.astype(f)*gain),0,255); G=np.clip(rint(g.astype(f)*gain),0,255); B=np.clip(rint(b.astype(f)*gain),0,255)
    return pack(R,G,B)

def pad_rows(img):
    return np.concatenate([img[:1],img,img[-1:]],0)

def detail_serial(img):
    r,g,b=split(img); L=luma_i(r,g,b)
    return detail_core(pad_rows(L), pad_rows(img), None)

class BoundsError(Exception): pass

def get_pixels(img, top, n):
    if top+n>img.shape[0]: raise BoundsError(f"y + height must be <= bitmap.height() (top={top}, n={n}, H={img.shape[0]})")
    return img[top:top+n].copy()

def detail_stripes(img, rows=64, fixed=True):
    H,W=img.shape; out=img.copy()
    carry=img[0:1].copy()
    top=0
    while top<H:
        rc=min(rows,H-top)
        src=np.zeros((rc+2,W),dtype=np.int64)
        src[0]=carry[0]
        n=rc+1 if not fixed else min(rc+1,H-top)
        src[1:1+n]=get_pixels(out, top, n)
        if fixed and n==rc: src[rc+1]=src[rc]
        r,g,b=split(src); L=luma_i(r,g,b)
        res=detail_core(L,src,None)
        carry=src[rc:rc+1].copy()
        out[top:top+rc]=res
        top+=rc
    return out

rng=np.random.default_rng(1)
H,W=150,97
base=np.zeros((H,W),dtype=np.int64)
yy,xx=np.mgrid[0:H,0:W]
lum=(60+40*np.sin(xx/7)+30*(xx>50)+rng.normal(0,6,(H,W))).clip(0,255)
base=pack(lum.astype(np.int64),(lum*0.9).astype(np.int64).clip(0,255),(lum*0.7).astype(np.int64).clip(0,255))
ref=detail_serial(base)
try:
    detail_stripes(base,64,fixed=False); print("OLD: no error?!")
except BoundsError as e: print("OLD code reproduces failure ->",e)
for rows in (64,50,150,7):
    out=detail_stripes(base,rows,fixed=True)
    print("fixed rows=%d identical to serial:"%rows, np.array_equal(out,ref))
