"""Convert the supplied Tripo GLB to NeoForge's built-in OBJ loader."""
import io,json,struct,sys
from pathlib import Path
import numpy as np
from PIL import Image
src=Path(sys.argv[1]); root=Path(__file__).resolve().parents[1]
f=io.BytesIO(src.read_bytes()); f.read(12); n,_=struct.unpack('<II',f.read(8)); d=json.loads(f.read(n)); n,_=struct.unpack('<II',f.read(8)); buf=f.read(n)
def acc(i):
 a=d['accessors'][i]; v=d['bufferViews'][a['bufferView']]; dim={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4}[a['type']]; dt={5126:'<f4',5125:'<u4',5123:'<u2',5121:'u1'}[a['componentType']]
 return np.ndarray((a['count'],dim),dtype=dt,buffer=buf,offset=v.get('byteOffset',0)+a.get('byteOffset',0),strides=(v.get('byteStride',np.dtype(dt).itemsize*dim),np.dtype(dt).itemsize)).copy()
p=d['meshes'][0]['primitives'][0]; pos=acc(p['attributes']['POSITION']); uv=acc(p['attributes']['TEXCOORD_0']); norm=acc(p['attributes']['NORMAL']); idx=acc(p['indices']).reshape(-1,3)
lo=pos.min(0); hi=pos.max(0); scale=min(.90/(hi[0]-lo[0]),.90/(hi[2]-lo[2]),1.85/(hi[1]-lo[1])); pos=(pos-np.array([(hi[0]+lo[0])/2,lo[1],(hi[2]+lo[2])/2]))*scale+np.array([.5,0,.5])
assets=root/'src/main/resources/assets/sworldwaystones'; out=assets/'models/block'; out.mkdir(parents=True,exist_ok=True)
with (out/'waystone.obj').open('w') as f:
 f.write('mtllib waystone.mtl\no waystone\n')
 for v in pos:f.write('v %.7f %.7f %.7f\n'%tuple(v))
 for v in uv:f.write('vt %.7f %.7f\n'%(v[0],1-v[1]))
 for v in norm:f.write('vn %.7f %.7f %.7f\n'%tuple(v))
 f.write('usemtl stone\n')
 for face in idx:f.write('f '+' '.join(f'{i+1}/{i+1}/{i+1}' for i in face)+'\n')
(out/'waystone.mtl').write_text('newmtl stone\nKa 0 0 0\nKd 1 1 1\nd 1\nmap_Kd #texture0\n')
tex=assets/'textures/block';tex.mkdir(parents=True,exist_ok=True)
im=d['images'][d['textures'][d['materials'][0]['pbrMetallicRoughness']['baseColorTexture']['index']]['source']];v=d['bufferViews'][im['bufferView']]; offset=v.get('byteOffset',0)
image=Image.open(io.BytesIO(buf[offset:offset+v['byteLength']]));image=image.convert('RGBA');image.thumbnail((1024,1024),Image.Resampling.LANCZOS);image.save(tex/'waystone.png')
print(f'{len(idx)} triangles; bounds {pos.min(0)} - {pos.max(0)}; texture {image.size}')
