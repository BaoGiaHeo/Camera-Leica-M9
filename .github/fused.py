import numpy as np
from ref import *   # detail_core, pack, split, luma_i, rint, detail_serial

f=np.float32
def denoise_core(src_rows, luma_rows, W):
    """src_rows/luma_rows have H+2 rows (halo). returns packed H rows. strength=2 (as Kotlin)."""
    H=src_rows.shape[0]-2
    ctr=src_rows[1:H+1]; cl=luma_rows[1:H+1]
    cr,cg,cb=split(ctr)
    rl=np.where(cl<72,27,20)
    sumR=np.zeros((H,W),np.int64);sumG=sumR.copy();sumB=sumR.copy();wsum=sumR.copy()
    xs=np.arange(W)
    for dy in (-1,0,1):
        for dx in (-1,0,1):
            if dx==0 and dy==0: continue
            nx=np.clip(xs+dx,0,W-1)
            nb=src_rows[1+dy:H+1+dy][:,nx]; nl=luma_rows[1+dy:H+1+dy][:,nx]
            nr,ng,nbb=split(nb)
            ld=np.abs(nl-cl)
            cd=np.maximum(np.abs(nr-cr),np.maximum(np.abs(ng-cg),np.abs(nbb-cb)))
            sp=2 if (dx==0 or dy==0) else 1
            w=np.where((ld<=rl)&(cd<=rl*2),sp,np.where((ld<=rl*2)&(cd<=rl*3),1,0))
            sumR+=nr*w;sumG+=ng*w;sumB+=nbb*w;wsum+=w
    shadow=np.clip(f(1.22)-cl.astype(f)/f(255)*f(0.42),f(0.80),f(1.22))
    blend=np.clip(f(0.30)*shadow,f(0.12),f(0.38))
    ws=np.where(wsum==0,1,wsum).astype(f)
    def mix(c,s):
        avg=s.astype(f)/ws
        return np.clip(rint(c.astype(f)*(f(1)-blend)+avg*blend),0,255)
    out=pack(mix(cr,sumR),mix(cg,sumG),mix(cb,sumB))
    return np.where(wsum==0,ctr,out)

def lum(img):
    r,g,b=split(img); return luma_i(r,g,b)

def sequential(img):
    """Original: whole-image denoise (edge replicate, 8888 store), then whole-image detail."""
    pad=np.concatenate([img[:1],img,img[-1:]],0)
    den=denoise_core(pad,lum(pad),img.shape[1])
    return detail_serial(den)

def fused(img, stripe_rows=128, chunk_rows=32):
    """New: stripes with 2-row halo, per-chunk denoise(+1 halo)+detail, boundary fix on denoised rows."""
    H,W=img.shape; out=np.zeros_like(img)
    for top in range(0,H,stripe_rows):
        rows=min(stripe_rows,H-top)
        # buffer row i <-> y = top-2+i, replicate-clamped
        ys=np.clip(np.arange(top-2,top+rows+2),0,H-1)
        buf=img[ys]
        for fr in range(0,rows,chunk_rows):
            ur=min(rows,fr+chunk_rows); rc=ur-fr
            src=buf[fr:fr+rc+4]; sl=lum(src)
            den=denoise_core(src,sl,W)          # rc+2 rows: y0-1 .. y0+rc
            if top+fr==0: den[0]=den[1]
            if top+ur==H: den[rc+1]=den[rc]
            res=detail_core(lum(den),den,None)  # rc rows
            out[top+fr:top+ur]=res
    return out

rng=np.random.default_rng(7)
for (H,W) in [(150,97),(64,64),(300,50),(130,33),(5,9),(3,3)]:
    yy,xx=np.mgrid[0:H,0:W]
    base=(70+50*np.sin(xx/5.0)+40*(yy>H//2)+rng.normal(0,9,(H,W))).clip(0,255)
    img=pack(base.astype(np.int64),(base*0.85).astype(np.int64),(base*0.6+rng.integers(0,12,(H,W))).astype(np.int64).clip(0,255))
    ref=sequential(img)
    ok=[np.array_equal(fused(img,sr,cr),ref) for sr,cr in [(128,32),(64,16),(128,7),(10,3),(1000,1000)] if sr>=2]
    print((H,W),"fused==sequential:",ok)
