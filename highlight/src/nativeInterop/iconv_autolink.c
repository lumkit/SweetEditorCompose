#if defined(__APPLE__)
/* ld64 honors LC_LINKER_OPTION inside static archives, so Xcode can
   resolve Oniguruma/SweetLine iconv refs without the host adding -liconv. */
asm(".linker_option \"-liconv\"");
#endif

void sweetline_iconv_autolink(void) {}
