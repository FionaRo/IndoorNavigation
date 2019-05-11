function intToBinary(id, len) {
    binary = [];
    for (let i = 0; i < len; i++) {
        binary.push(id % 2);
        id >>= 1;
    }
    return binary.reverse();
}

function getMatrixHamming(width, height) {
    let matrix = [];

    for (let i = 0; i < width; i++) {
        matrix.push([]);
        let t = i + 1;
        for (let j = 0; j < height; j++) {
            matrix[i].push(t % 2);
            t >>= 1;
        }
    }

    matrix = matrix[0].map((col, i) => matrix.map(row => row[i]));
    return matrix;
}

function encodeHamming(binary) {
    let extendedArr = [];
    let baseArrIndex = 0;
    let twoPow = 1;
    let powsIndex = [];
    while (baseArrIndex < binary.length) {
        if (extendedArr.length + 1 == twoPow) {
            powsIndex.push(extendedArr.length);
            extendedArr.push(0);
            twoPow *= 2;
        } else {
            extendedArr.push(binary[baseArrIndex]);
            baseArrIndex++;
        }
    }

    let matrix = getMatrixHamming(extendedArr.length, powsIndex.length)
    let powsNum = []

    for (let i = 0; i < powsIndex.length; i++) {
        let sum = 0;
        for (let j = 0; j < extendedArr.length; j++) {
            sum += extendedArr[j] & matrix[i][j];
        }
        powsNum.push(sum % 2);
    }

    for (let i = 0; i < powsIndex.length; i++) {
        extendedArr[powsIndex[i]] = powsNum[i];
    }

    return extendedArr;
}

function addPreambule(binary) {
    let preambule = [1, 0, 1, 0, 1, 0, 1, 0];
    return preambule.concat(binary);
}

let currentState = 0
let tact = 3000
let i = 0

function setInitState() {
    if (i == code.length)
        i = 0;

    if (currentState == code[i]) {
        if (code[i] === 0) {
            changeBackground(1);
            currentState = 1;
        }
        else {
            changeBackground(0)
            currentState = 0;
        }
    }

    setTimeout(setState, tact / 2);
}

function setState() {
    if (i == code.length)
        i = 0;

    changeBackground(code[i]);
    setTimeout(setInitState, tact / 2);

    currentState = code[i];
    i++;
}

function changeBackground(state) {
    if (state === 0) {
        document.getElementsByTagName("body")[0].style.backgroundColor = "black";
    } else {
        document.getElementsByTagName("body")[0].style.backgroundColor = "white";
    }
}

let k = 0

function time() {
    k++;
    if (k<45)
        setTimeout(time, 600);
    else
        console.log("END")
}

const ID = 13;
let code = intToBinary(ID, 8)
code = encodeHamming(code)
code = addPreambule(code)

setInitState()