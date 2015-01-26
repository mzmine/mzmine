//-----------------------------------------------------------------------------------------------------
// FILE:			MassLynxRawDefs.h
// DATE:			Nov 2009
// COPYRIGHT(C):	Waters Corporation
//					
//-----------------------------------------------------------------------------------------------------
#ifdef MASSLYNX_RAW_EXPORTS
#define MLYNX_RAW_API __declspec(dllexport)
#define EXPIMP_TEMPLATE
#else
#define MLYNX_RAW_API __declspec(dllimport)
#define EXPIMP_TEMPLATE extern
#endif