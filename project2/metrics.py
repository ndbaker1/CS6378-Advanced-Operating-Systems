from pathlib import Path
import matplotlib.pyplot as plt

# Hold average value of message count, throughput, response time for different parameters
mc, tp, rt = {}, {}, {}
# How many times a certain paramter setting appears
count = {}


for child in Path('metrics').iterdir():
    if child.is_file():
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
    mc[key] = mc[key] / c
    tp[key] = tp[key] / c
    rt[key] = rt[key] / c

# Values for x axis
x = [0,2,4,6,8,10]

# Values for mc, tp, rt when varying d/c
mc_d, tp_d, rt_d = [], [], []
mc_c, tp_c, rt_c = [], [], []

# Vary Inter-Request delay (d): n=8, d=0-10, c=5
for d in x:
    mc_d.append(mc[f"8:{d}:5"])
    tp_d.append(tp[f"8:{d}:5"])
    rt_d.append(rt[f"8:{d}:5"])

# Vary CS Execution Time (c): n=8, d=5, c=0-10
for c in x:
    mc_c.append(mc[f"8:5:{c}"])
    tp_c.append(tp[f"8:5:{c}"])
    rt_c.append(rt[f"8:5:{c}"])

figure, axis = plt.subplots(2, 3)

axis[0,0].set_title("Message Complexity vs Inter-Request Delay")
axis[0,0].set_xlabel('Inter-Request Delay (ms)')
axis[0,0].set_ylabel('Message Complexity')
axis[0,0].plot(x, mc_d)
  
axis[0,1].set_title("Throughput vs Inter-Request Delay")
axis[0,1].set_xlabel('Inter-Request Delay (ms)')
axis[0,1].set_ylabel('Throughput (requests/s)')
axis[0,1].plot(x, tp_d)
  
axis[0,2].set_title("Response Time vs Inter-Request Delay")
axis[0,2].set_xlabel('Inter-Request Delay (ms)')
axis[0,2].set_ylabel('Response Time (ms)')
axis[0,2].plot(x, rt_d)
  
axis[1,0].set_title("Message Complexity vs CS Execution Time")
axis[1,0].set_xlabel('CS Execution Time (ms)')
axis[1,0].set_ylabel('Message Complexity')
axis[1,0].plot(x, mc_c)
  
axis[1,1].set_title("Throughput vs CS Execution Time")
axis[1,1].set_xlabel('CS Execution Time (ms)')
axis[1,1].set_ylabel('Throughput (requests/s)')
axis[1,1].plot(x, tp_c)
  
axis[1,2].set_title("Response Time vs CS Execution Time")
axis[1,2].set_xlabel('CS Execution Time (ms)')
axis[1,2].set_ylabel('Response Time (ms)')
axis[1,2].plot(x, rt_c)

plt.show()