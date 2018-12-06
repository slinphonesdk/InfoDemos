package com.bean.common;

public class SSIDUtils
{

    private static long[] crc_32_tab = { /* CRC polynomial 0xedb88320 */
            0x2bf32d7a,0x64806f53,0x57197f37,0x3ffe0bf2,0x102e5263,
            0x497b6bb9,0x6c5c7a90,0x0d0d15f7,0x130609a6,0x7ccd7cf1,
            0x13ac5e36,0x15da1ed7,0x530f6c0a,0x56a132f7,0x53967301,
            0x6a5b3731,0x5cf1181a,0x2dd671ab,0x46fc14ce,0x71793ac6,
            0x6be670a3,0x1c832350,0x059278fe,0x15934afb,0x7a3d6b38,
            0x146d7b0d,0x5c6204f5,0x40fb595a,0x21da6eb1,0x4392476f,
            0x5648349c,0x2a774211,0x4cb441a8,0x48a254e6,0x147610e0,
            0x71c17671,0x355608fb,0x37806044,0x76f13631,0x0c083770,
            0x375f564d,0x6dc51941,0x15126b04,0x006d6028,0x70014685,
            0x373e34f9,0x06064651,0x74874056,0x2e85304d,0x4269106b,
            0x794f6615,0x08df06dd,0x34c66294,0x64fc62fc,0x1f4135d2,
            0x05743c40,0x47b2730c,0x305e00f3,0x6186450a,0x56384d2b,
            0x0fcc1cad,0x562c2dfb,0x6b9a2dc8,0x579124ea,0x49ac2e30,
            0x5cb57fa4,0x034f0b17,0x72da57f0,0x467d0895,0x237e4fe2,
            0x314a7f88,0x0ae05e52,0x3c7f2d9e,0x37bf3f8d,0x39844bb0,
            0x38655e49,0x67835e52,0x78e448df,0x63ca74a8,0x56a006b4,
            0x7971497b,0x0d0d61f8,0x143d7a8b,0x5fba7994,0x5ac54a27,
            0x258f2f5d,0x33d31efc,0x1953468e,0x3b0d362b,0x13704675,
            0x68d53b9e,0x10fb756c,0x716d3c9b,0x2bef000b,0x1f5f2899,
            0x68b9209c,0x2d1c5453,0x7cfa5acf,0x61e3331c,0x73881fb0,
            0x75e43a72,0x51b66dd5,0x01a623d5,0x21d541dc,0x7ca600da,
            0x06fa6b15,0x0f41557b,0x3e02187b,0x3bd32cb1,0x2fd431e9,
            0x23215b45,0x78e6518a,0x67f464e2,0x0a037192,0x76073770,
            0x464b222b,0x3023309d,0x427b5bb2,0x498921bc,0x096e4124,
            0x4e9045d3,0x09e466d3,0x208f5def,0x1183715e,0x68fd67bb,
            0x791b20df,0x47d72251,0x662e1c66,0x41425550,0x45b011a7,
            0x2e720e2f,0x640b7cef,0x35ca47d1,0x46662d5d,0x0e453059,
            0x2927594e,0x1214013f,0x47b65719,0x64891da4,0x51844dfd,
            0x2f336ae2,0x304d7357,0x3a4b1768,0x039a2818,0x644e43c5,
            0x75890a77,0x3cec4202,0x4cd04fe3,0x47215f35,0x60007b3c,
            0x26a26b53,0x4e074342,0x3c7a7346,0x1ff93d4e,0x76f5613f,
            0x38102040,0x58bd774e,0x31f133b8,0x7b435830,0x18303f89,
            0x10601268,0x13170f6a,0x273d6191,0x74a35eef,0x387019f2,
            0x45d041b8,0x3d677253,0x39165be9,0x570a200f,0x26347ed9,
            0x479c796b,0x732c7e0f,0x43e21226,0x0e8f506a,0x5f880651,
            0x5efa519f,0x18c06761,0x4be137e9,0x472c7f83,0x438b11fb,
            0x71fe5f2f,0x625e613a,0x515e6900,0x1f5d2429,0x7f0c4fc3,
            0x21ef6524,0x1a463ace,0x24e22152,0x62ec0493,0x0aaf1bd5,
            0x0fe24774,0x07007143,0x64b62cd9,0x016f7f5a,0x4a823280,
            0x159174ef,0x70145a15,0x54331e28,0x354d7703,0x29e340eb,
            0x65c70e89,0x0eb31d96,0x06ba7e10,0x633967f1,0x3c215bbc,
            0x1eda4c61,0x09184541,0x3349555e,0x6f8910f8,0x494a4526,
            0x33071532,0x3abc37b3,0x42fa79cc,0x6dd620eb,0x0ffd4803,
            0x75b17b18,0x50842c8a,0x1c063997,0x78c21fda,0x383a53d4,
            0x69ca68ca,0x77977c76,0x1bf42d5f,0x7add58d6,0x488205e5,
            0x6cf94cb6,0x5683463e,0x77142e30,0x0ef8717b,0x35cf75ec,
            0x5c360ba9,0x13cd3f96,0x188b3ded,0x2d732e8f,0x302400d4,
            0x21eb0ae3,0x1a2e40e4,0x756f2a7c,0x6d3c3177,0x68c47a95,
            0x12e941c9,0x6a043d71,0x5ebe684f,0x02a1504c,0x0c56384a,
            0x66a70d92,0x68bf759d,0x09411f4f,0x2b241adb,0x5a7f55e6,
            0x40a905a5,0x12565707,0x0f973d33,0x2ce938db,0x75b90730,
            0x7c065d81,0x06063ecb,0x71f422be,0x497f220b,0x096c39f0,
            0x075b0962,0x6ee53c44,0x58623d86,0x4f154f29,0x09975f7c,
            0x6eb46d05,0x121916f5,0x6f832626,0x204b7ade,0x668d6dfb,
            0x02336833,0x27ac4884,0x6d203b14,0x6a506c07,0x68901e1a,
            0x2b7c2b05,0x605063eb,0x71113a6c,0x21e51f0c,0x524f280e,
            0x492b6bf7,0x69991552,0x603c216e,0x13780a29,0x36935b0e,
            0x1ee37444,0x33721227,0x52be2685,0x278038cb,0x6ca06cb3,
            0x61b70b25,0x7cd90771,0x0b7d0b0c,0x43b42471,0x101159d8,
            0x73f926e5,0x13296475,0x7eb65436,0x3ada15b6,0x4c3f38f9,
            0x50b702eb,0x06745a1f,0x7e3d46b5,0x5842066b,0x286f7e19,
            0x466d5c2a,0x2ebd1bd4,0x058c571e,0x112a05ee,0x71557abb,
            0x41ae3192,0x091a6c96,0x24d9311d,0x1594131a,0x05ba6875,
            0x5be00c7c,0x00ff5b9c,0x01c711f2,0x2b5d4b85,0x07421b20,
            0x77331526,0x793d55d3,0x1d6a06ce,0x419c45f2,0x0d9f1fb9,
            0x387c21b2,0x29846de2,0x5abb3e08,0x4f837e18,0x49ba1f64,
            0x78a51254,0x7861029c,0x6db3124a,0x29647226,0x579b6a3e,
            0x22c53b8b,0x2f144c19,0x3a9b5918,0x4f8506bc,0x211123d7,
            0x31236794,0x0bb161be,0x0e5a5a6e,0x25e30425,0x786f3b8d,
            0x6ab02a7e,0x1f630ffa,0x4bcc3d40,0x511a181c,0x49d43524,
            0x73bd7088,0x61d46294,0x7f5c351a,0x740c4960,0x3ad85a45,
            0x07f464b4,0x400030a7,0x117d6747,0x0afa46e1,0x1b9a4bd4,
            0x18e51da5,0x7bff49ce,0x59bc6211,0x74280250,0x797d223b,
            0x639e7953,0x448b300d,0x6f762f23,0x2b512653,0x431a6e14,
            0x29294412,0x4e4c2070,0x127c121b,0x246742b5,0x0e2a06e1,
            0x60e845fd,0x3e2c1c88,0x352e76be,0x5b780d3e,0x037e3bb3,
            0x492429ce,0x2b4a411a,0x62cf7e72,0x6ab36420,0x0a3d39e9,
            0x794c729a,0x38410aca,0x402112f5,0x14e06718,0x52f86870,
            0x5ab37216,0x0ed64788,0x098d745d,0x01c50eb4,0x2c5e1a2b,
            0x7fd57753,0x36576bfe,0x096172e3,0x3b4f3562,0x66936873,
            0x07664e07,0x1c2a665b,0x59e87916,0x1d7b1c2a,0x47744ec5,
            0x5fb630e0,0x075d642a,0x326b495e,0x3050254c,0x014d531a,
    };

    private static long crc32(long crc, String buffer, int size) {

        for (int i = 0; i < size; i++) {
            crc = crc_32_tab[((int)crc ^ buffer.charAt(i)) & 0xff] ^ (crc >> 8);
        }
        return crc;
    }



    private static String ssID;
    private static int getSSIDLength ( int area, int room)
    {
        ssID = String.format("SYC-%d-%d", area, room);
        return ssID.length();
    }

    public static String getSSIDPassword (int area, int room)
    {

        int length = getSSIDLength(area, room);
        long len = crc32(0xFFFFFFFFL, ssID, length);
        return String.format("%08X", len);
    }

}