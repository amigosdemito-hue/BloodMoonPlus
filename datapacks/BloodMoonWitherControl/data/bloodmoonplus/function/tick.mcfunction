# ============================================================
#  BloodMoonPlus - Control de Wither
#  Se ejecuta cada tick (ver data/minecraft/tags/function/tick.json)
#
#  Mata cualquier Wither que NO tenga la etiqueta "bloodmoon_boss".
#  El plugin BloodMoonPlus añade esa etiqueta únicamente al jefe
#  de la Luna de Sangre (Azote Carmesí), por lo que solo ese Wither
#  puede sobrevivir. Cualquier otro Wither (invocado por jugador,
#  huevos, comandos, granjas, etc.) es eliminado al instante.
# ============================================================

execute as @e[type=minecraft:wither, tag=!bloodmoon_boss] at @s run function bloodmoonplus:wither_kill
