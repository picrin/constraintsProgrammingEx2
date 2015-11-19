import sys
def median(lista):
    lista.sort()
    ll = len(lista)
    if ll % 2 == 1:
         return (lista[ll/2] + lista[ll/2 + 1])/2
    else:
         return lista[ll/2]
for filename in sys.argv[1:]:
    with open(filename) as re:
        lista = re.readlines()
        times = [float(elem) for index, elem in enumerate(lista) if index%2 == 1]
        nodes = [float(elem) for index, elem in enumerate(lista) if index%2 == 0]
        print("avg for time ", filename, " " , sum(times)/len(times))
        print("median for time", filename, " ", median(times))
        print("avg for nodes ", filename, " " , sum(nodes)/len(nodes))
        print("median for nodes", filename, " ", median(nodes))
        print("unsolved in 59 seconds", filename, " ", len([time for time in times if time > 59]))
