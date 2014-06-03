//
//	Slaut - Slamnig Audio Utilities project
//
// wsola.h
//
// WSOLA audio time stretching function header
//
// Created: 2014/05/18 D.Slamnig
//

void WSOLA_Init(void);
void WSOLA_Close(void);
int WSOLA_TimeStretch(short *psInBuf, int nInBufLen, 
	short *psOutBuf, int nOutBufLen, 
	int nInRate, bool bStereo, float fSpeedRatio, int nDecimate);
