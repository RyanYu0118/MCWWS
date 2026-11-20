import json,heapq,math,collections
from pathlib import Path
import numpy as np
from scipy import ndimage as ndi
from PIL import Image,ImageDraw
P=Path('tools/mcwws-build-bridge-mcp/purple-coast-expansion-20261105');meta=json.loads((P/'palette.json').read_text());names=meta['names'];ids={n:i for i,n in enumerate(names)}
g=np.load(P/'grid-before.npy',mmap_mode='r');S=np.load(P/'surface.npz');H,W=g.shape[0],g.shape[2];zz,xx=np.mgrid[2940:2940+H,-920:-920+W];bed=S['bed'].astype(float)
marks=json.loads((P/'purple-markers.json').read_text());m=np.zeros((H,W),bool)
for a in marks:m[a['z']-2940,a['x']+920]=True
lab,n=ndi.label(m,np.ones((3,3)));lineid=lab[3024-2940,-418+920];line=lab==lineid
selected=[a for a in marks if line[a['z']-2940,a['x']+920]]
neighbors=[(a,b,math.hypot(a,b)) for a in [-1,0,1] for b in [-1,0,1] if a or b]
def path(mask,start,end):
 pq=[(0.,0.,start)];cost={start:0.};parent={}
 while pq:
  _,c,p=heapq.heappop(pq)
  if c!=cost[p]:continue
  if p==end:
   out=[p]
   while p!=start:p=parent[p];out.append(p)
   return out[::-1]
  z,x=p
  for dz,dx,w in neighbors:
   nz,nx=z+dz,x+dx
   if 0<=nz<H and 0<=nx<W and mask[nz,nx]:
    q=(nz,nx);v=c+w
    if v<cost.get(q,1e99):cost[q]=v;parent[q]=p;heapq.heappush(pq,(v+math.hypot(nz-end[0],nx-end[1]),v,q))
 raise RuntimeError('No connecting path')
start=(3024-2940,-418+920);end=(3271-2940,-814+920);guidepath=path(line,start,end)
water=g[:,126,:]==ids['minecraft:water'];wl,n=ndi.label(water,np.ones((3,3)));main=wl==wl[3100-2940,-850+920];wz,wx=np.where(main)
def nearest(p):
 i=np.argmin((wz-p[0])**2+(wx-p[1])**2);return (int(wz[i]),int(wx[i]))
ws,we=nearest(start),nearest(end);oldpath=path(main,we,ws)
im=Image.new('1',(W,H));ImageDraw.Draw(im).polygon([(x,z) for z,x in guidepath+oldpath],fill=1);polygon=np.array(im,bool)
polygon|=line
# The guide marks the exact inland limit; no change is allowed outside this mask.
guidecoords={(a['z']-2940,a['y']+64,a['x']+920) for a in selected}
craftids=[i for i,n in enumerate(names) if any(k in n for k in ['_planks','_stairs','_slab','_fence','_door','_trapdoor','lantern','iron_chain','barrel','chest','rail','stone_bricks','tuff_bricks','polished','_wool','iron_bars','_cauldron','bricks'])]
craft=np.isin(g,craftids)
for z,y,x in guidecoords:craft[z,y,x]=False
# Protect docks, ships and all structures which extend into the above-water zone.
storage_ids=[ids[n] for n in ['minecraft:chest','minecraft:trapped_chest','minecraft:barrel'] if n in ids]
storage=np.isin(g[:,45+64:95+64,:],storage_ids).any(axis=1)
core_craft=craft[:,63+64:95+64,:].copy()
for i in storage_ids:core_craft[g[:,63+64:95+64,:]==i]=False
above_craft=core_craft.any(axis=1)
wood_ids=[i for i,n in enumerate(names) if n.endswith(('_log','_wood'))]
above_craft |= np.isin(g[:,63+64:95+64,:],wood_ids).any(axis=1)
protect=(ndi.distance_transform_edt(~above_craft)<=9)|(ndi.distance_transform_edt(~storage)<=3)
newland=polygon&~water&~protect
domain=polygon&(ndi.distance_transform_edt(~newland)<=65)&~protect
domain &= ((zz>=3024)|(water))
# Original sea surface is retained except in the actual newly inundated area.
shore_dist=ndi.distance_transform_edt(polygon)
line_dist=ndi.distance_transform_edt(~line)
native0=np.load('tools/mcwws-build-bridge-mcp/ocean-bed-refinement-20261105/design.npz')['native']
native=native0[zz-2800,np.clip(xx+1200,0,native0.shape[1]-1)]
native=ndi.gaussian_filter(native,1.5)
distance,near=ndi.distance_transform_edt(domain,return_indices=True);boundary=bed[tuple(near)]
weight=np.clip(distance/65,0,1);h=boundary*(1-weight)+native*weight;h[~domain]=bed[~domain]
highestcraft=np.where(craft[:,:126,:],np.arange(-64,62)[None,:,None],-999).max(axis=1)
required=np.where(domain,np.minimum(highestcraft-1,bed),-999)
# The guide boundary becomes a shallow shelf, with a wide natural descent offshore.
shore_requirement=61-1.35*line_dist
required=np.maximum(required,np.where(domain,shore_requirement,-999))
lo=np.full((H,W),-64.);hi=np.full((H,W),61.)
for dz,dx in [(-1,0),(1,0),(0,-1),(0,1)]:
 outside=np.roll(~domain,(dz,dx),(0,1));b=np.roll(bed,(dz,dx),(0,1));tol=np.maximum(2,np.abs(bed-b));edge=domain&outside
 lo[edge]=np.maximum(lo[edge],(b-tol)[edge]);hi[edge]=np.minimum(hi[edge],(b+tol)[edge])
lo=np.maximum(lo,required);hi=np.maximum(hi,lo)
alpha=.012*np.minimum(distance/40,1)**2
for i in range(1100):
 nei=ndi.convolve(h,np.array([[0,1,0],[1,0,1],[0,1,0]],float),mode='nearest');new=(nei+alpha*native)/(4+alpha);h[domain]=np.clip(new,lo,hi)[domain]
for i in range(180):
 lower=ndi.maximum_filter(h,footprint=np.array([[0,1,0],[1,0,1],[0,1,0]],bool),mode='nearest')-1.6;new=np.clip(np.maximum(h,lower),lo,hi);h[domain]=new[domain]
target=np.rint(np.clip(h,-35,61)).astype(np.int16);target[~domain]=bed[~domain].astype(np.int16)
# Identify the first existing walkable air pocket. Its roof is a hard ceiling.
airids=[ids.get('minecraft:'+n,-1) for n in ['air','cave_air','void_air']]
air=np.isin(g[:,63+64:111+64,:],airids)
first=np.full((H,W),-1,np.int16);run=np.zeros((H,W),np.int16)
for i in range(air.shape[1]):
 run=np.where(air[:,i,:],run+1,0);hit=(first<0)&(run>=5);first[hit]=63+i-4
ceiling=np.full((H,W),320,np.int16)
for y in range(63,160):
 solid=~np.isin(g[:,y+64,:],airids);hit=(first>=0)&(y>=first)&solid&(ceiling==320);ceiling[hit]=y
noise=ndi.gaussian_filter(np.random.default_rng(631105).normal(size=(H,W)),9);noise/=noise.std()
clear_top=np.rint(80+np.minimum(line_dist*.12,12)+noise*1.7).astype(np.int16)
clear_top=np.maximum(clear_top,first-1);clear_top=np.minimum(clear_top,ceiling-1);clear_top=np.clip(clear_top,70,94)
# Existing low roofs are preserved exactly, including roof faces below Y70.
clear_top=np.where((ceiling<95)&(first>=0),np.minimum(clear_top,ceiling-1),clear_top)
np.savez_compressed(P/'design.npz',polygon=polygon,line=line,guidepath=np.asarray(guidepath),oldpath=np.asarray(oldpath),domain=domain,newland=newland,protect=protect,target=target,native=native,clear_top=clear_top,existing_roof=ceiling,line_dist=line_dist)
(P/'selected-guide.json').write_text(json.dumps(selected));report={'guide_blocks':len(selected),'guide_columns':int(line.sum()),'guide_path_columns':len(guidepath),'old_sea_connection_endpoints':[[ws[1]-920,ws[0]+2940],[we[1]-920,we[0]+2940]],'polygon_columns':int(polygon.sum()),'new_land_columns':int(newland.sum()),'bed_domain_columns':int(domain.sum()),'protected_columns_in_polygon':int((protect&polygon).sum()),'bed_target_percentiles':np.quantile(target[domain],[0,.1,.5,.9,1]).tolist(),'modified_xz_bounds':[int(xx[domain].min()),int(xx[domain].max()),int(zz[domain].min()),int(zz[domain].max())],'carve_max_y':int(clear_top[domain].max()),'upper_cliff_preserved_from_y':95}
(P/'design-report.json').write_text(json.dumps(report,indent=2));print(json.dumps(report),flush=True)
v=np.clip(S['top']*.8+35,20,240).astype('uint8');rgb=np.stack([v,v,v],axis=2);rgb[water]=[25,85,145];rgb[domain]=[25,130,200];rgb[newland]=[25,185,215];rgb[protect&polygon]=[240,145,25];rgb[line]=[220,25,230]
im=Image.fromarray(rgb).resize((W*2,H*2));dr=ImageDraw.Draw(im)
for x in range(-900,-359,50):
 px=(x+920)*2;dr.line((px,0,px,H*2),fill=(70,70,80));dr.text((px+2,5),str(x),fill='white')
for z in range(2950,3361,50):
 pz=(z-2940)*2;dr.line((0,pz,W*2,pz),fill=(70,70,80));dr.text((2,pz+2),str(z),fill='white')
im.save(P/'design.png')
