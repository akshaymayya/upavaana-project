import Image from "next/image";

const steps = [
  "IDEA",
  "Website",
  "Hosting",
  "Payments",
  "Marketing",
  "Emails",
  "Analytics",
  "Growth",
  "Scale",
];

export function ProcessFlow() {
  return (
    <section className="relative px-4 pb-16 sm:px-5">
      {/* Mobile: stacked flow */}
      <div className="flex flex-col gap-3 md:hidden">
        {steps.map((label, index) => (
          <div
            key={label}
            className={`flex h-[47px] items-center rounded-[29px] bg-[#ffdcdc] px-5 ${
              index === steps.length - 1 ? "justify-end" : "justify-start"
            }`}
          >
            <span
              className={`text-[28px] font-bold text-[#464b45] ${
                label === "Payments" ? "italic" : ""
              }`}
            >
              {label}
            </span>
          </div>
        ))}
      </div>

      {/* Desktop: pixel-aligned flow diagram */}
      <div className="relative mx-auto hidden min-h-[640px] max-w-[633px] md:block">
        <Image
          src="/images/vectors-flow.png"
          alt=""
          width={585}
          height={556}
          className="absolute left-[17px] top-0"
          aria-hidden
        />

        <div className="absolute left-[5px] top-0 flex h-[47px] w-[123px] items-center rounded-[29px] bg-[#ffdcdc] px-4">
          <span className="text-[37px] font-bold text-[#464b45]">IDEA</span>
        </div>
        <div className="absolute left-0 top-[58px] flex h-[40px] w-[325px] items-center rounded-[29px] bg-[#ffdcdc] px-4">
          <span className="text-[37px] font-bold text-[#464b45]">Website</span>
        </div>
        <div className="absolute left-[45%] top-[109px] flex h-[47px] w-[177px] items-center rounded-[29px] bg-[#ffdcdc] px-4">
          <span className="text-[37px] font-bold text-[#464b45]">Hosting</span>
        </div>
        <div className="absolute left-[10%] top-[185px] flex h-[47px] w-[484px] max-w-[95%] items-center rounded-[29px] bg-[#ffdcdc] px-4">
          <span className="text-[37px] font-bold italic text-[#464b45]">Payments</span>
        </div>
        <div className="absolute left-[25%] top-[236px] flex h-[47px] w-[210px] items-center rounded-[29px] bg-[#ffdcdc] px-4">
          <span className="text-[37px] font-bold text-[#464b45]">Marketing</span>
        </div>
        <div className="absolute left-[5px] top-[306px] flex h-[47px] w-[177px] items-center rounded-[29px] bg-[#ffdcdc] px-4">
          <span className="text-[37px] font-bold text-[#464b45]">Emails</span>
        </div>
        <div className="absolute left-[15%] top-[371px] flex h-[47px] w-[542px] max-w-[95%] items-center rounded-[29px] bg-[#ffdcdc] px-4">
          <span className="text-[37px] font-bold text-[#464b45]">Analytics</span>
        </div>
        <div className="absolute left-[55%] top-[456px] flex h-[47px] w-[177px] items-center rounded-[29px] bg-[#ffdcdc] px-4">
          <span className="text-[37px] font-bold text-[#464b45]">Growth</span>
        </div>
        <div className="absolute left-0 top-[554px] flex h-[47px] w-full items-center justify-end rounded-[29px] bg-[#ffdcdc] px-4">
          <span className="text-[37px] font-bold text-[#464b45]">Scale</span>
        </div>

        <Image
          src="/images/vector-271.png"
          alt=""
          width={71}
          height={34}
          className="absolute left-[55%] top-[132px]"
          aria-hidden
        />
        <Image
          src="/images/vector-272.png"
          alt=""
          width={44}
          height={40}
          className="absolute left-[48%] top-[145px]"
          aria-hidden
        />
      </div>
    </section>
  );
}
