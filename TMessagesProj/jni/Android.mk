LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_PRELINK_MODULE := false
LOCAL_MODULE 	:= tmessages
LOCAL_CFLAGS 	:= -w -std=gnu99 -O2 -DNULL=0 -DSOCKLEN_T=socklen_t -DLOCALE_NOT_USED -D_LARGEFILE_SOURCE=1 -D_FILE_OFFSET_BITS=64
LOCAL_CFLAGS 	+= -Drestrict='' -D__EMX__ -DOPUS_BUILD -DFIXED_POINT -DUSE_ALLOCA -DHAVE_LRINT -DHAVE_LRINTF -fno-math-errno
LOCAL_CFLAGS 	+= -DANDROID_NDK -DDISABLE_IMPORTGL -fno-strict-aliasing -fprefetch-loop-arrays -DAVOID_TABLES -DANDROID_TILE_BASED_DECODE -DANDROID_ARMV6_IDCT
LOCAL_CPPFLAGS 	:= -DBSD=1 -ffast-math -O2 -funroll-loops
#LOCAL_LDLIBS 	:= -llog
LOCAL_LDLIBS 	:= -ljnigraphics -llog

LOCAL_SRC_FILES     := \
./opus/src/opus.c \
./opus/src/opus_decoder.c \
./opus/src/opus_encoder.c \
./opus/src/opus_multistream.c \
./opus/src/opus_multistream_encoder.c \
./opus/src/opus_multistream_decoder.c \
./opus/src/repacketizer.c \
./opus/src/analysis.c \
./opus/src/mlp.c \
./opus/src/mlp_data.c

LOCAL_SRC_FILES     += \
./opus/silk/CNG.c \
./opus/silk/code_signs.c \
./opus/silk/init_decoder.c \
./opus/silk/decode_core.c \
./opus/silk/decode_frame.c \
./opus/silk/decode_parameters.c \
./opus/silk/decode_indices.c \
./opus/silk/decode_pulses.c \
./opus/silk/decoder_set_fs.c \
./opus/silk/dec_API.c \
./opus/silk/enc_API.c \
./opus/silk/encode_indices.c \
./opus/silk/encode_pulses.c \
./opus/silk/gain_quant.c \
./opus/silk/interpolate.c \
./opus/silk/LP_variable_cutoff.c \
./opus/silk/NLSF_decode.c \
./opus/silk/NSQ.c \
./opus/silk/NSQ_del_dec.c \
./opus/silk/PLC.c \
./opus/silk/shell_coder.c \
./opus/silk/tables_gain.c \
./opus/silk/tables_LTP.c \
./opus/silk/tables_NLSF_CB_NB_MB.c \
./opus/silk/tables_NLSF_CB_WB.c \
./opus/silk/tables_other.c \
./opus/silk/tables_pitch_lag.c \
./opus/silk/tables_pulses_per_block.c \
./opus/silk/VAD.c \
./opus/silk/control_audio_bandwidth.c \
./opus/silk/quant_LTP_gains.c \
./opus/silk/VQ_WMat_EC.c \
./opus/silk/HP_variable_cutoff.c \
./opus/silk/NLSF_encode.c \
./opus/silk/NLSF_VQ.c \
./opus/silk/NLSF_unpack.c \
./opus/silk/NLSF_del_dec_quant.c \
./opus/silk/process_NLSFs.c \
./opus/silk/stereo_LR_to_MS.c \
./opus/silk/stereo_MS_to_LR.c \
./opus/silk/check_control_input.c \
./opus/silk/control_SNR.c \
./opus/silk/init_encoder.c \
./opus/silk/control_codec.c \
./opus/silk/A2NLSF.c \
./opus/silk/ana_filt_bank_1.c \
./opus/silk/biquad_alt.c \
./opus/silk/bwexpander_32.c \
./opus/silk/bwexpander.c \
./opus/silk/debug.c \
./opus/silk/decode_pitch.c \
./opus/silk/inner_prod_aligned.c \
./opus/silk/lin2log.c \
./opus/silk/log2lin.c \
./opus/silk/LPC_analysis_filter.c \
./opus/silk/LPC_inv_pred_gain.c \
./opus/silk/table_LSF_cos.c \
./opus/silk/NLSF2A.c \
./opus/silk/NLSF_stabilize.c \
./opus/silk/NLSF_VQ_weights_laroia.c \
./opus/silk/pitch_est_tables.c \
./opus/silk/resampler.c \
./opus/silk/resampler_down2_3.c \
./opus/silk/resampler_down2.c \
./opus/silk/resampler_private_AR2.c \
./opus/silk/resampler_private_down_FIR.c \
./opus/silk/resampler_private_IIR_FIR.c \
./opus/silk/resampler_private_up2_HQ.c \
./opus/silk/resampler_rom.c \
./opus/silk/sigm_Q15.c \
./opus/silk/sort.c \
./opus/silk/sum_sqr_shift.c \
./opus/silk/stereo_decode_pred.c \
./opus/silk/stereo_encode_pred.c \
./opus/silk/stereo_find_predictor.c \
./opus/silk/stereo_quant_pred.c

LOCAL_SRC_FILES     += \
./opus/silk/fixed/LTP_analysis_filter_FIX.c \
./opus/silk/fixed/LTP_scale_ctrl_FIX.c \
./opus/silk/fixed/corrMatrix_FIX.c \
./opus/silk/fixed/encode_frame_FIX.c \
./opus/silk/fixed/find_LPC_FIX.c \
./opus/silk/fixed/find_LTP_FIX.c \
./opus/silk/fixed/find_pitch_lags_FIX.c \
./opus/silk/fixed/find_pred_coefs_FIX.c \
./opus/silk/fixed/noise_shape_analysis_FIX.c \
./opus/silk/fixed/prefilter_FIX.c \
./opus/silk/fixed/process_gains_FIX.c \
./opus/silk/fixed/regularize_correlations_FIX.c \
./opus/silk/fixed/residual_energy16_FIX.c \
./opus/silk/fixed/residual_energy_FIX.c \
./opus/silk/fixed/solve_LS_FIX.c \
./opus/silk/fixed/warped_autocorrelation_FIX.c \
./opus/silk/fixed/apply_sine_window_FIX.c \
./opus/silk/fixed/autocorr_FIX.c \
./opus/silk/fixed/burg_modified_FIX.c \
./opus/silk/fixed/k2a_FIX.c \
./opus/silk/fixed/k2a_Q16_FIX.c \
./opus/silk/fixed/pitch_analysis_core_FIX.c \
./opus/silk/fixed/vector_ops_FIX.c \
./opus/silk/fixed/schur64_FIX.c \
./opus/silk/fixed/schur_FIX.c

LOCAL_SRC_FILES     += \
./opus/celt/bands.c \
./opus/celt/celt.c \
./opus/celt/celt_encoder.c \
./opus/celt/celt_decoder.c \
./opus/celt/cwrs.c \
./opus/celt/entcode.c \
./opus/celt/entdec.c \
./opus/celt/entenc.c \
./opus/celt/kiss_fft.c \
./opus/celt/laplace.c \
./opus/celt/mathops.c \
./opus/celt/mdct.c \
./opus/celt/modes.c \
./opus/celt/pitch.c \
./opus/celt/celt_lpc.c \
./opus/celt/quant_bands.c \
./opus/celt/rate.c \
./opus/celt/vq.c \
./opus/celt/arm/armcpu.c \
./opus/celt/arm/arm_celt_map.c

LOCAL_SRC_FILES     += \
./opus/ogg/bitwise.c \
./opus/ogg/framing.c \
./opus/opusfile/info.c \
./opus/opusfile/internal.c \
./opus/opusfile/opusfile.c \
./opus/opusfile/stream.c

LOCAL_SRC_FILES     += \
./giflib/dgif_lib.c \
./giflib/gifalloc.c

LOCAL_SRC_FILES     += \
./aes/aes_core.c \
./aes/aes_ige.c \
./aes/aes_misc.c

LOCAL_SRC_FILES     += \
./sqlite/sqlite3.c

LOCAL_C_INCLUDES    := \
./opus/include \
./opus/silk \
./opus/silk/fixed \
./opus/celt \
./opus/ \
./opus/opusfile \
./libyuv/include

LOCAL_SRC_FILES     += \
./libjpeg/jcapimin.c \
./libjpeg/jcapistd.c \
./libjpeg/armv6_idct.S \
./libjpeg/jccoefct.c \
./libjpeg/jccolor.c \
./libjpeg/jcdctmgr.c \
./libjpeg/jchuff.c \
./libjpeg/jcinit.c \
./libjpeg/jcmainct.c \
./libjpeg/jcmarker.c \
./libjpeg/jcmaster.c \
./libjpeg/jcomapi.c \
./libjpeg/jcparam.c \
./libjpeg/jcphuff.c \
./libjpeg/jcprepct.c \
./libjpeg/jcsample.c \
./libjpeg/jctrans.c \
./libjpeg/jdapimin.c \
./libjpeg/jdapistd.c \
./libjpeg/jdatadst.c \
./libjpeg/jdatasrc.c \
./libjpeg/jdcoefct.c \
./libjpeg/jdcolor.c \
./libjpeg/jddctmgr.c \
./libjpeg/jdhuff.c \
./libjpeg/jdinput.c \
./libjpeg/jdmainct.c \
./libjpeg/jdmarker.c \
./libjpeg/jdmaster.c \
./libjpeg/jdmerge.c \
./libjpeg/jdphuff.c \
./libjpeg/jdpostct.c \
./libjpeg/jdsample.c \
./libjpeg/jdtrans.c \
./libjpeg/jerror.c \
./libjpeg/jfdctflt.c \
./libjpeg/jfdctfst.c \
./libjpeg/jfdctint.c \
./libjpeg/jidctflt.c \
./libjpeg/jidctfst.c \
./libjpeg/jidctint.c \
./libjpeg/jidctred.c \
./libjpeg/jmemmgr.c \
./libjpeg/jmemnobs.c \
./libjpeg/jquant1.c \
./libjpeg/jquant2.c \
./libjpeg/jutils.c

LOCAL_SRC_FILES     += \
./libyuv/source/compare_common.cc \
./libyuv/source/compare_neon.cc \
./libyuv/source/compare_posix.cc \
./libyuv/source/compare_win.cc \
./libyuv/source/compare.cc \
./libyuv/source/convert_argb.cc \
./libyuv/source/convert_from_argb.cc \
./libyuv/source/convert_from.cc \
./libyuv/source/convert_jpeg.cc \
./libyuv/source/convert_to_argb.cc \
./libyuv/source/convert_to_i420.cc \
./libyuv/source/convert.cc \
./libyuv/source/cpu_id.cc \
./libyuv/source/format_conversion.cc \
./libyuv/source/mjpeg_decoder.cc \
./libyuv/source/mjpeg_validate.cc \
./libyuv/source/planar_functions.cc \
./libyuv/source/rotate_argb.cc \
./libyuv/source/rotate_mips.cc \
./libyuv/source/rotate_neon.cc \
./libyuv/source/rotate_neon64.cc \
./libyuv/source/rotate.cc \
./libyuv/source/row_any.cc \
./libyuv/source/row_common.cc \
./libyuv/source/row_mips.cc \
./libyuv/source/row_neon.cc \
./libyuv/source/row_neon64.cc \
./libyuv/source/row_posix.cc \
./libyuv/source/row_win.cc \
./libyuv/source/scale_argb.cc \
./libyuv/source/scale_common.cc \
./libyuv/source/scale_mips.cc \
./libyuv/source/scale_neon.cc \
./libyuv/source/scale_neon64.cc \
./libyuv/source/scale_posix.cc \
./libyuv/source/scale_win.cc \
./libyuv/source/scale.cc \
./libyuv/source/video_common.cc

LOCAL_SRC_FILES     += \
./jni.c \
./sqlite_cursor.c \
./sqlite_database.c \
./sqlite_statement.c \
./sqlite.c \
./audio.c \
./gif.c \
./utils.c \
./image.c \
./video.c \
./fake.c

include $(BUILD_SHARED_LIBRARY)