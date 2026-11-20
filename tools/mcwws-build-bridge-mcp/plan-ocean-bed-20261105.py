import json,collections
from pathlib import Path
import numpy as np
from scipy import ndimage as ndi
from PIL import Image,ImageDraw
P=Path('tools/mcwws-build-bridge-mcp/ocean-bed-refinement-20261105');meta=json.loads((P/'palette.json').read_text());names=meta['names'];ids={n:i for i,n in enumerate(names)}
g=np.load(P/'grid-before.npy',mmap_mode='r');bed=np.load(P/'bed.npy').astype(float);H,W=bed.shape;zz,xx=np.mgrid[2800:2800+H,-1200:-1200+W]
old=np.load('tools/mcwws-build-bridge-mcp/ocean-restoration-20261105/design.npz');base=(old['cut']|old['restore'])[:H,:W]
ocean=np.isin(g[:,126,:],[ids.get('minecraft:'+n,-1) for n in ['water','bubble_column','ice','packed_ice','blue_ice']])
port=(xx>=-725)&(zz<=3160)
domain=(ndi.distance_transform_edt(~base)<=32)&ocean&~port
domain &= (xx>=-1170)&(xx<=-655)&(zz>=2820)&(zz<=3450)
domain[:2,:]=False;domain[-2:,:]=False;domain[:,:2]=False;domain[:,-2:]=False
# The reference is intact terrain immediately around the player's inspection point.
sample=bed[60:220,305:445].copy();valid=(sample<30)&(sample>-35)
_,ind=ndi.distance_transform_edt(~valid,return_indices=True);sample[~valid]=sample[tuple(ind[:,~valid])]
sample=ndi.gaussian_filter(sample,1.4)
cy=.62*(zz-2860)+17*np.sin(xx/85)+12*np.cos(zz/113)
cx=.63*(xx+895)+19*np.sin(zz/77)+9*np.cos(xx/93)
native=ndi.map_coordinates(sample,[cy,cx],order=3,mode='reflect')
native=np.clip(native,-24,24)
# Source-noise sculpting with a screened Laplace solve keeps the actual surrounding
# terrain as a hard boundary, instead of tapering toward an arbitrary raised rim.
distance,near=ndi.distance_transform_edt(domain,return_indices=True)
boundary=bed[tuple(near)]
weight=np.clip(distance/70,0,1);weight=weight*weight*(3-2*weight)
h=boundary*(1-weight)+native*weight;h[~domain]=bed[~domain]
# Structures in the seabed remain supported. Deep mineshafts below the new bed
# need no additional anchor; exposed structures retain their existing foundation.
crafted_ids=[i for i,n in enumerate(names) if any(k in n for k in ['_planks','_stairs','_slab','_fence','_door','_trapdoor','lantern','iron_chain','barrel','chest','rail','stone_bricks','tuff_bricks','polished','_wool','iron_bars','_cauldron','bricks']) or n in ['minecraft:cobblestone','minecraft:mossy_cobblestone']]
crafted=np.isin(g[:,:126,:],crafted_ids)
highestcraft=np.where(crafted,np.arange(-64,62)[None,:,None],-999).max(axis=1)
anchor=(highestcraft>native+2)&domain
required=np.where(anchor,np.minimum(highestcraft-1,bed),-999)
fixed=~domain
# Boundary intervals prevent creation of a new ledge at the edge of the sculpt.
lo=np.full((H,W),-64.);hi=np.full((H,W),61.)
for dz,dx in [(-1,0),(1,0),(0,-1),(0,1)]:
 outside=np.roll(~domain,(dz,dx),(0,1));b=np.roll(bed,(dz,dx),(0,1));tol=np.maximum(2,np.abs(bed-b))
 edge=domain&outside
 lo[edge]=np.maximum(lo[edge],(b-tol)[edge]);hi[edge]=np.minimum(hi[edge],(b+tol)[edge])
lo=np.maximum(lo,required);lo=np.minimum(lo,bed);hi=np.maximum(hi,bed)
adist=ndi.distance_transform_edt(~anchor)
alpha=.012*np.minimum(distance/45,1)**2*np.minimum(adist/32,1)**2
for i in range(1400):
 neighbor=ndi.convolve(h,np.array([[0,1,0],[1,0,1],[0,1,0]],float),mode='nearest')
 nxt=(neighbor+alpha*native)/(4+alpha)
 h[~fixed]=np.clip(nxt,lo,hi)[~fixed]
# A slope envelope spreads structural foundations and high shoreline anchors
# over a broad gradient. Existing native boundary ledges are never made larger.
for i in range(180):
 lower=ndi.maximum_filter(h,footprint=np.array([[0,1,0],[1,0,1],[0,1,0]],bool),mode='nearest')-1.6
 new=np.clip(np.maximum(h,lower),lo,hi)
 h[~fixed]=new[~fixed]
h=np.rint(np.clip(h,-42,61)).astype(np.int16);h[~domain]=bed[~domain].astype(np.int16)
# Keep the natural reference itself unchanged.
reference=(xx>=-870)&(xx<=-770)&(zz>=2900)&(zz<=3000)
reference &= ~base
assert not domain[2966-2800,-832+1200]
np.savez_compressed(P/'design.npz',domain=domain,target=h,native=native,anchor=anchor,port=port,reference=reference,distance=distance)
stats={}
for name,field in [('before',bed),('target',h)]:
 dx=np.abs(np.diff(field,axis=1));dz=np.abs(np.diff(field,axis=0));ex=domain[:,1:]|domain[:,:-1];ez=domain[1:,:]|domain[:-1,:]
 edges=np.r_[dx[ex],dz[ez]]
 seamx=domain[:,1:]^domain[:,:-1];seamz=domain[1:,:]^domain[:-1,:];seam=np.r_[dx[seamx],dz[seamz]]
 stats[name]={'height_percentiles':np.quantile(field[domain],[0,.1,.5,.9,1]).tolist(),'neighbor_height_percentiles':np.quantile(edges,[.5,.9,.99,1]).tolist(),'edges_over_8':int((edges>8).sum()),'seam_percentiles':np.quantile(seam,[.5,.9,.99,1]).tolist(),'seam_edges_over_8':int((seam>8).sum())}
report={'columns':int(domain.sum()),'anchor_columns':int(anchor.sum()),'bounds':[int(xx[domain].min()),int(xx[domain].max()),int(zz[domain].min()),int(zz[domain].max())],'stats':stats}
(P/'design-report.json').write_text(json.dumps(report,indent=2));print(json.dumps(report),flush=True)
v=np.clip((h+40)*2.4,0,255).astype('uint8');rgb=np.stack([v,255-v,np.full_like(v,130)],axis=2);im=Image.fromarray(rgb).resize((W*2,H*2));d=ImageDraw.Draw(im)
for x in range(-1200,-559,100):
 px=(x+1200)*2;d.line((px,0,px,H*2),fill=(90,90,90));d.text((px+2,5),str(x),fill='white')
for z in range(2800,3461,100):
 pz=(z-2800)*2;d.line((0,pz,W*2,pz),fill=(90,90,90));d.text((2,pz+2),str(z),fill='white')
im.save(P/'bed-design.png')
