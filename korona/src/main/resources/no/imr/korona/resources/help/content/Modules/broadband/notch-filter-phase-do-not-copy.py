# coding: utf-8
import matplotlib
import matplotlib.pyplot as plt
import numpy as np

###
# Script for generation of phase response of notch filter
###

plt.rcdefaults()

font = {'size': 15}

matplotlib.rc('font', **font)

# define png dpi and resolution
my_dpi = 100
x_res, y_res = 600, 448


# define notch filter transfer function
def notch(f, f_rej, q):
    fc = 1j*f
    return (fc**2 + f_rej**2) / (fc**2 + f_rej / q * fc + f_rej**2)


f = np.linspace(0.1, 10, 100000)

plt.figure(figsize=(x_res/my_dpi, y_res/my_dpi), dpi=my_dpi)
plt.semilogx(f, np.angle(notch(f, 1, 0.5), deg=True), 'b', linewidth=4, zorder=200)
plt.plot([1, 1], [-120, 120], 'r', linewidth=2, zorder=3)
plt.plot([0.43, 0.43], [-120, 120], 'r--', zorder=2)
plt.plot([2.5, 2.5], [-120, 120], 'r--', zorder=2)
# plt.annotate('-3 dB', xy=(4, -4), xytext=(5, -40),
#              arrowprops=dict(facecolor='black', shrink=0.05),
#              )
plt.annotate('rejection\nfrequency', xy=(1.1, -100), xytext=(2, -100),
             arrowprops=dict(facecolor='red', shrink=0.05),
             )
plt.text(0.72, 105, 'bandwidth', zorder=2000)
plt.annotate('', xy=(0.43, 100), xytext=(2.5, 100), arrowprops=dict(arrowstyle='<|-|>', facecolor='black'))
plt.ylim([-120, 120])
plt.xlabel('Normalized frequency')
plt.ylabel('Phase [deg]')
plt.title('Frequency response')
plt.tight_layout()
plt.savefig('notch-filter-phase.svg', dpi=my_dpi,
            metadata={'Creator': None, 'Date': None, 'Format': None, 'Type': None})
plt.show()
