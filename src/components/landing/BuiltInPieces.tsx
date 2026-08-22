import Image from "next/image";
import { Logo } from "./Logo";

export function BuiltInPieces() {
  return (
    <section className="relative px-4 pb-20 pt-8 sm:px-5">
      <div className="relative mx-auto max-w-[633px]">
        <div className="relative">
          <Image
            src="/images/window-landscape.png"
            alt="Sunset through an arched window"
            width={590}
            height={332}
            className="mx-auto h-auto w-full max-w-[590px] object-cover"
          />

          <p className="absolute left-2 top-4 text-[28px] font-bold text-white sm:text-[34px]">
            NOT JUST
          </p>
          <p className="absolute right-2 top-2 text-[42px] font-bold text-white sm:text-[59px]">
            GROW
          </p>
        </div>

        <div className="relative mt-6 grid min-h-[320px] grid-cols-2 gap-4 sm:min-h-[360px]">
          <div className="relative">
            <div className="flex flex-col gap-1 pl-2 pt-2">
              {["NOT JUST", "NOT JUST", "NOT JUST"].map((text, i) => (
                <p
                  key={`${text}-${i}`}
                  className="text-[22px] font-bold text-transparent sm:text-[34px]"
                  style={{ WebkitTextStroke: "1px rgba(255,255,255,0.35)" }}
                >
                  {text}
                </p>
              ))}
            </div>
            <div className="absolute bottom-0 left-0 h-[200px] w-[120px] rounded-[40px] bg-[#ffcd84] sm:h-[316px] sm:w-[191px] sm:rounded-[60px]" />
            <p className="absolute bottom-4 left-6 text-[22px] font-bold text-white sm:text-[34px]">
              YOU
            </p>
          </div>

          <div className="relative">
            <p className="mb-4 text-[22px] font-bold text-white sm:text-[34px]">YOU</p>
            <div
              className="ml-auto h-[180px] w-[120px] rounded-[40px] sm:h-[297px] sm:w-[180px] sm:rounded-[56px]"
              style={{
                backgroundImage:
                  "linear-gradient(180deg, rgb(77, 80, 70) 0%, rgb(255, 204, 132) 73.72%)",
              }}
            />
            <div className="absolute bottom-8 left-1/4 flex size-[60px] items-center justify-center rounded-br-[24px] rounded-tl-[24px] bg-[#4d5046] sm:size-[71px] sm:rounded-br-[28px] sm:rounded-tl-[28px]">
              <Logo variant="dark" />
            </div>
            <div className="absolute bottom-0 right-0 h-[200px] w-[120px] rounded-[40px] bg-[#ffcd84] sm:h-[316px] sm:w-[191px] sm:rounded-[60px]" />
          </div>
        </div>

        <p className="mt-6 text-center text-[22px] font-bold text-white sm:text-left sm:text-[38px]">
          BUILD
        </p>

        <p className="mt-6 text-right text-[16px] font-bold leading-tight text-[#4d5046] sm:text-[23px]">
          BUSINESSES ARE NOT BUILT IN A NIGHT
          <br />
          BUT IN PIECES
        </p>
      </div>
    </section>
  );
}
