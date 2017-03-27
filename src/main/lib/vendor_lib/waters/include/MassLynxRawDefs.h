//-----------------------------------------------------------------------------------------------------
// FILE:			MassLynxRawDefs.h
// COPYRIGHT(C):	Waters Corporation
//					TODO:	Further documentation
//					
//-----------------------------------------------------------------------------------------------------
#ifdef MASSLYNX_RAW_EXPORTS
#define MLYNX_RAW_API __declspec(dllexport)
#else
#define MLYNX_RAW_API __declspec(dllimport)
#endif
