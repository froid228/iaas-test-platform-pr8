"""Схемы архитектуры и жизненного цикла для отчёта (не скриншоты)."""
from pathlib import Path
import shutil
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch

OUT = Path(__file__).resolve().parent / 'screenshots'
OUT.mkdir(parents=True, exist_ok=True)
plt.rcParams.update({'font.family':'DejaVu Sans','font.size':10})


def box(ax, x, y, width, height, text, color='#f1f4f6'):
    ax.add_patch(FancyBboxPatch((x-width/2,y-height/2),width,height,
        boxstyle='round,pad=0.06',linewidth=1,edgecolor='#243644',facecolor=color))
    ax.text(x,y,text,ha='center',va='center',fontsize=10)


def arrow(ax, start, end, label=None, style='-'):
    ax.annotate('',xy=end,xytext=start,arrowprops={'arrowstyle':'->','color':'#243644','lw':1.2,'linestyle':style})
    if label:
        x,y=(start[0]+end[0])/2,(start[1]+end[1])/2
        ax.text(x,y+0.13,label,ha='center',va='bottom',fontsize=8,
            bbox={'facecolor':'white','edgecolor':'none','pad':1})


fig,ax=plt.subplots(figsize=(12,8.5))
ax.set_xlim(0,16);ax.set_ylim(0,11);ax.axis('off')
box(ax,2,10,3.1,.7,'REST-клиент / demo.py')
box(ax,7,10,3.0,.7,'Keycloak / OAuth2')
arrow(ax,(3.65,10),(5.4,10),'JWT')
box(ax,12.8,10,4.0,.7,'Config Server ← Git / main')
ax.text(12.8,9.25,'Конфигурация всех приложений',ha='center',fontsize=8)
box(ax,2,7.9,3.1,.8,'API Gateway\nпорт 8088')
arrow(ax,(2,9.6),(2,8.4),'Bearer JWT + requestId')
box(ax,7,7.9,3.0,.8,'Eureka\nService Discovery')
arrow(ax,(3.65,7.9),(5.4,7.9),'lb://', '--')
box(ax,12.8,7.2,3.4,.9,'RabbitMQ\nplatform.events')
for x,name,db in [(2,'project-service','projects'),(7,'environment-service','environments'),(12.8,'test-run-service','test_runs')]:
    box(ax,x,4.5,3.5,.8,name)
    box(ax,x,2.7,3.5,.7,'PostgreSQL: '+db)
    arrow(ax,(x,4.05),(x,3.1),'JPA / Flyway')
arrow(ax,(2,7.43),(2,4.96),'HTTP')
arrow(ax,(3.1,7.43),(6,4.96))
arrow(ax,(3.55,7.43),(11.3,4.96))
# Feign-вызовы проведены ниже сервисов с отдельными линиями.
ax.plot([12.8,12.8,7],[4.02,3.8,3.8],color='#243644',lw=1.2)
arrow(ax,(7,3.8),(7,4.02))
ax.text(10,3.52,'Feign + JWT + requestId',ha='center',fontsize=8,bbox={'facecolor':'white','edgecolor':'none','pad':1})
ax.plot([12.3,12.3,2],[4.02,3.5,3.5],color='#243644',lw=1.2)
arrow(ax,(2,3.5),(2,4.02))
# Событие: producer вверх к брокеру, consumer отдельно слева от брокера.
arrow(ax,(12.8,4.96),(12.8,6.68),'run.created / after commit')
ax.plot([11.03,9.4,9.4,7],[7.2,7.2,5.9,5.9],color='#243644',lw=1.2,linestyle='--')
arrow(ax,(7,5.9),(7,4.96),style='--')
ax.text(10,7.45,'environment.run-created',ha='center',fontsize=8,bbox={'facecolor':'white','edgecolor':'none','pad':1})
ax.text(8,1.3,'HTTP и Feign — синхронные вызовы; RabbitMQ — асинхронное уведомление.\n'
    'Каждый предметный сервис использует отдельный экземпляр PostgreSQL.',ha='center',fontsize=9)
fig.tight_layout(pad=.5)
fig.savefig(OUT/'01.png',dpi=190,facecolor='white')
fig.savefig(OUT/'architecture.svg',facecolor='white')
plt.close(fig)

fig,ax=plt.subplots(figsize=(10,3.5))
ax.set_xlim(0,12);ax.set_ylim(0,5);ax.axis('off')
for x,y,name in [(1.5,3.5,'QUEUED'),(5.4,3.5,'RUNNING'),(10,4.15,'PASSED'),(10,2.7,'FAILED'),(5.4,1,'CANCELLED')]:
    box(ax,x,y,2.4,.7,name)
arrow(ax,(2.8,3.5),(4.1,3.5),'/start')
arrow(ax,(6.7,3.7),(8.65,4.1),'/result')
arrow(ax,(6.7,3.3),(8.65,2.75),'/result')
arrow(ax,(5.4,3.08),(5.4,1.4),'/cancel')
arrow(ax,(1.5,3.08),(4.1,1.2),'/cancel')
fig.tight_layout(pad=.4)
fig.savefig(OUT/'02.png',dpi=190,facecolor='white')
fig.savefig(OUT/'run-lifecycle.svg',facecolor='white')
plt.close(fig)
shutil.copyfile(OUT/'eureka.png',OUT/'03.png')
for svg in OUT.glob('*.svg'):
    svg.write_text('\n'.join(line.rstrip() for line in svg.read_text().splitlines()) + '\n')
print('Diagrams and Eureka figure ready')
