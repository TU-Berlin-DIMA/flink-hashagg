# create folder
mkdir ./results/comparison/
# combine imgs
for DS in A1 A2 A3; do for T in runtimes tuples emissions; do
  montage -geometry +0+0 ./results/ex-$DS.X.v$VER.$ENV/plots/$T.png ./results/ex-$DS.Y.v$VER.$ENV/plots/$T.png ./results/comparison/ex-$DS.v$VER.$ENV.$T.png
done; done
