# coding: utf-8
import matplotlib
import matplotlib.pyplot as plt
import numpy as np

###
# Script for generation of magnitude response of ideal bandpass filter
###

plt.rcdefaults()

font = {'size': 15}

matplotlib.rc('font', **font)

# define png dpi and resolution
my_dpi = 100
x_res, y_res = 600, 448


# define ideal bandpass filter transfer function
def ideal_bandpass(f, f_min, f_max, dist):
    out = np.zeros_like(f)
    out[np.logical_and(f_min <= f, f <= f_max)] = 1
    out[np.logical_and(f_min - dist <= f, f <= f_min)] = 0.5 * (1 + np.cos(np.pi * (f_min - f[np.logical_and(f_min - dist <= f, f <= f_min)]) / dist))
    out[np.logical_and(f_max <= f, f <= f_max + dist)] = 0.5 * (1 + np.cos(np.pi * (f_max - f[np.logical_and(f_max <= f, f <= f_max + dist)]) / dist))
    return out


f = np.linspace(0, 100, 1000)

plt.figure(figsize=(x_res/my_dpi, y_res/my_dpi), dpi=my_dpi)
plt.plot(f, 20 * np.log(np.abs(np.fft.irfft(np.fft.rfft(ideal_bandpass(f, 30, 70, 20))))), 'b', linewidth=4, zorder=200)
plt.plot([30, 30], [-120, 120], 'r', linewidth=2, zorder=3)
plt.plot([70, 70], [-120, 120], 'r', linewidth=2, zorder=3)
plt.plot([10, 10], [-120, 120], 'r--', zorder=2)
plt.plot([90, 90], [-120, 120], 'r--', zorder=2)
plt.annotate('startFrequency', xy=(30.1, -60), xytext=(40, -60),
             arrowprops=dict(facecolor='red', shrink=0.05),
             )
plt.annotate('stopFrequency', xy=(69.9, -30), xytext=(25, -30),
             arrowprops=dict(facecolor='red', shrink=0.05),
             )
plt.annotate('', xy=(20, 11), xytext=(29, 20),
             arrowprops=dict(facecolor='black', shrink=0.05),
             )
plt.annotate('', xy=(80, 11), xytext=(71, 20),
             arrowprops=dict(facecolor='black', shrink=0.05),
             )
plt.text(29.5, 20, 'stopBandDistance', zorder=2000)
plt.annotate('', xy=(10, 10), xytext=(30, 10), arrowprops=dict(arrowstyle='<|-|>', facecolor='black'))
plt.annotate('', xy=(70, 10), xytext=(90, 10), arrowprops=dict(arrowstyle='<|-|>', facecolor='black'))
plt.ylim([-80, 30])
plt.xlabel('Frequency [kHz]')
plt.ylabel('Magnitude [dB]')
plt.title('Frequency response')
plt.tight_layout()
plt.savefig('bandpass-filter-magnitude.svg', dpi=my_dpi,
            metadata={'Creator': None, 'Date': None, 'Format': None, 'Type': None})
plt.show()
