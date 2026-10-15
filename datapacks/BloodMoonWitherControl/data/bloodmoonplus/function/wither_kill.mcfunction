# Elimina al Wither no autorizado (@s) que invocó el jugador.
tellraw @a[distance=..64] [{"text":"[Luna de Sangre] ","color":"dark_red","bold":true},{"text":"El Wither está vetado. Solo el jefe de la Luna de Sangre puede aparecer.","color":"red"}]
particle minecraft:explosion_emitter ~ ~1 ~ 0 0 0 0 1
playsound minecraft:entity.wither.death hostile @a[distance=..64] ~ ~ ~ 0.8 1.2
kill @s
