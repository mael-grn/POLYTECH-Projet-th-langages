main: XOR R0 R0 R0
ADDi R0 R0 42
XOR R1 R1 R1
ADDi R1 R1 1
ADDi R2 R1 0
XOR R5 R5 R5
start_while_2: JEQU R2 R5 end_while_2
ADDi R3 R1 0
XOR R4 R4 R4
ADDi R4 R4 0
JMP start_while_2
end_while_2: XOR R5 R5 R5
ADDi R6 R4 0
