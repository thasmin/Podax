/*
	check_neon: check NEON availability

	copyright 1995-2014 by the mpg123 project - free software under the terms of the LGPL 2.1
	see COPYING and AUTHORS files in distribution or http://mpg123.org
	initially written by Taihei Momma
*/

#include "mangle.h"

	.code 32
#ifndef __APPLE__
	.fpu neon
#endif

	.text
	.globl ASM_NAME(check_neon)
#ifdef __ELF__
	.type ASM_NAME(check_neon), %function
#endif
	ALIGN4
ASM_NAME(check_neon):
	vorr	d0, d0, d0
	bx		lr

NONEXEC_STACK
