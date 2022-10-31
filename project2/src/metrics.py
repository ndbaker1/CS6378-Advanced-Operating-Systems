from pathlib import Path
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D

# Hold average value of message count, throughput, response time for different parameters
mc, tp, rt = {}, {}, {}
# How many times a certain paramter setting appears
count = {}
m = {}


for child in Path('metrics').iterdir():
    if child.is_file():
        # print(f"{child.name}:\n{child.read_text()}\n")
        with child.open() as f:
            for line in f.readlines():
                entry = line.split()
                count[entry[0]] = count.get(entry[0], 0) + 1 # Increase count of this parameter occurring

                # Sum value for metric
                if entry[1] == "messageComplexity":
                    mc[entry[0]] = mc.get(entry[0], 0) + float(entry[2])
                elif entry[1] == "systemThroughput":
                    tp[entry[0]] = tp.get(entry[0], 0) + float(entry[2])
                elif entry[1] == "responseTime":
                    rt[entry[0]] = rt.get(entry[0], 0) + float(entry[2])


# Average the sum of each metric
for key in mc:
    c = count[key] / 3 # Divide by 3 since count is incremented once for each metric in the log
    m[key] = (mc[key]/c, tp[key]/c, rt[key]/c)
    mc[key] = mc[key] / c
    tp[key] = tp[key] / c
    rt[key] = rt[key] / c

print(mc)
print(tp)
print(rt)
print(m)


ax = plt.gca()
ax2 = ax.twinx()

# Vary Message Count: n=3-4, d=200, c=100
nstart = 3
nstop = 4
d, c = 200, 100
x = list(range(nstart, nstop+1))
mc_y, tp_y, rt_y = [], [], []
for n in x:
    entry = m[f"{n}:{d}:{c}"]
    mc_y.append(entry[0])
    tp_y.append(entry[1])
    rt_y.append(entry[2])

print(x)
print(mc_y)
print(tp_y)
print(rt_y)

ax.plot(x, mc_y, label="message complexity", color="red")
ax.plot(x, tp_y, label="throughput", color="green")
ax2.plot(x, rt_y, label="response time", color="blue")

ax.set_xlabel('Number of Nodes') 
ax.set_ylabel('count') 
ax2.set_ylabel('milliseconds') 
  
plt.title('Varying Number of Nodes')
  
# Custom legend since using multiple axes to avoid multiple legends
custom_lines = [Line2D([0], [0], color="red", lw=4),
                Line2D([0], [0], color="green", lw=4),
                Line2D([0], [0], color="blue", lw=4)]
ax.legend(custom_lines, ['Message Complexity', 'Throughput', 'Response Time'])
  
plt.show()