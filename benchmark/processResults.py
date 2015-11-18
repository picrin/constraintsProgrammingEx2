import sys
def median(lista):
    ll = len(lista)
    if ll % 2 == 1:
         return (lista[ll/2] + lista[ll/2 + 1])/2
    else:
         return lista[ll/2]
with open(sys.argv[1]) as re:
    lista = re.readlines()
    times = [float(elem) for index, elem in enumerate(lista) if index%2 == 1]
    nodes = [float(elem) for index, elem in enumerate(lista) if index%2 == 0]
    print("avg for time ", sys.argv[1], " " , sum(times)/len(times))
    print("median for time", sys.argv[1], " ", median(times))
    print("avg for nodes ", sys.argv[1], " " , sum(nodes)/len(nodes))
    print("median for nodes", sys.argv[1], " ", median(nodes))
    print("max for time", sys.argv[1], " ", max(times))
    print("max for nodes", sys.argv[1], " ", max(nodes))
