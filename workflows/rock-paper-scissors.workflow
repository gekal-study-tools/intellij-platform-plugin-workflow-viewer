name 'Rock Paper Scissors'

steps {
    step('start', type: Start)
    step('rock_paper_scissors') {
        actions('storeHands', function: 'StoreData', oneTime: false) {
            string 'player1_hand'
            string 'player2_hand'
        }
    }
    step('draw', type: End)
    step('player1_wins', type: End)
    step('player2_wins', type: End)
}

transitions {
    transition(from: 'start', to: 'rock_paper_scissors') { true }
    transition(from: 'rock_paper_scissors', to: 'draw') {
        iterm('player1_hand') == iterm('player2_hand')
    }
    transition(from: 'rock_paper_scissors', to: 'player1_wins') {
        (iterm('player1_hand') == 'rock' && iterm('player2_hand') == 'scissors')
        || (iterm('player1_hand') == 'paper' && iterm('player2_hand') == 'rock')
        || (iterm('player1_hand') == 'scissors' && iterm('player2_hand') == 'paper')
    }
    transition(from: 'rock_paper_scissors', to: 'player2_wins') {
        (iterm('player1_hand') == 'scissors' && iterm('player2_hand') == 'rock')
        || (iterm('player1_hand') == 'paper' && iterm('player2_hand') == 'scissors')
        || (iterm('player1_hand') == 'rock' && iterm('player2_hand') == 'paper')
    }
}